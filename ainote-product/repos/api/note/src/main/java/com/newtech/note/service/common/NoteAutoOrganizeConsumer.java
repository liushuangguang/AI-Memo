package com.newtech.note.service.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.entity.pojo.NoteId;
import io.lettuce.core.RedisCommandTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import static com.newtech.note.common.constant.RedisConstant.GROUP_NAME;
import static com.newtech.note.common.constant.RedisConstant.STREAM_NAME;

@Service
public class NoteAutoOrganizeConsumer {
    private static final Logger logger = LogManager.getLogger(NoteAutoOrganizeConsumer.class);
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${note.auto-organize.consumer.name:c1}")
    private String consumerName;


    public NoteAutoOrganizeConsumer(ReactiveStringRedisTemplate reactiveStringRedisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = reactiveStringRedisTemplate;
        this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 监听流数据，当应用准备就绪后调用
     */
    //@EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        ensureStreamExists(STREAM_NAME)
                .then(createConsumerGroupIfNotExists(STREAM_NAME, GROUP_NAME))
                .thenMany(listenToStream())
                .subscribe();
    }

    public Mono<Void> ensureStreamExists(String streamName) {

        return redisTemplate.opsForStream().size(streamName)
                .flatMap(size -> {
                    if (size > 0) {
                        logger.info("Stream {} already exists with {} entries", streamName, size);
                        return Mono.empty();
                    }
                    // 如果 Stream 不存在或为空，创建Stream并添加第一个消息
                    logger.info("Stream {} does not exist, creating a new one", streamName);
                    return redisTemplate.opsForStream()
                            .add(ObjectRecord.create(streamName, new NoteId("123")))
                            .doOnSuccess(recordId -> logger.info("Stream {} created and message added with ID: {} ", streamName, recordId))
                            .then();
                });

    }

    public Mono<Void> createConsumerGroupIfNotExists(String streamName, String groupName) {
        return redisTemplate.opsForStream()
                .groups(streamName)
                .filter(group -> group.groupName().equals(groupName))
                .hasElements()
                .flatMap(exists -> {
                    if (!exists) {
                        return redisTemplate.opsForStream()
                                .createGroup(streamName, groupName);
                    }
                    return Mono.empty(); // 如果组已存在，返回空
                })
                .then();
    }

    @SuppressWarnings("unchecked")
    public Flux<NoteId> listenToStream() {
        Consumer consumer = Consumer.from(GROUP_NAME, consumerName);
        StreamOffset<String> streamOffset = StreamOffset.create(STREAM_NAME, ReadOffset.lastConsumed());
        // Duration.ZERO Duration.ofMillis(0)) 表示使用阻塞读取，永久阻塞，直到有消息到来

        StreamReadOptions readOptions = StreamReadOptions.empty().block(Duration.ofMillis(0)/*Duration.ofSeconds(60)*/);
        return redisTemplate.opsForStream()
                .read(consumer, readOptions, streamOffset)
                .repeat() // 继续监听新的消息 ,确保处理完当前数据后继续监听后续数据
                .flatMap(record -> {
                    // 收到消息后进行处理
                    Map<Object, Object> value = record.getValue();
                    NoteId noteId = objectMapper.convertValue(value, NoteId.class);
                    logger.info("Received noteId: " + noteId.getId());
                    // TODO: 处理消息
                    // 处理完成后确认消息，防止重复消费
                    return redisTemplate.opsForStream().acknowledge(STREAM_NAME, GROUP_NAME, record.getId()).then(Mono.just(noteId));
                })
                .onErrorResume(throwable -> {

                    if (throwable instanceof RedisCommandTimeoutException || throwable instanceof QueryTimeoutException) {
                        // 重新调用 read 方法继续监听
                        return listenToStream();
                    }
                    logger.error("Error while listening to stream", throwable);
                    return Flux.error(throwable);
                });
    }
}
