package com.newtech.note.service.common;

import com.newtech.note.entity.pojo.NoteId;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.Record;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@Service
public class RedisStreamObjectConsumer implements StreamListener<String, Record<String, Map<String, NoteId>>> {
    private static final Logger logger = LogManager.getLogger(RedisStreamObjectConsumer.class);

    private final ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate;

    public RedisStreamObjectConsumer(@Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }


    public Mono<Void> createConsumerGroupIfNotExists() {
        return reactiveRedisTemplate.opsForStream()
                .groups("test2")
                .filter(group -> group.groupName().equals("g2"))
                .hasElements()
                .flatMap(exists -> {
                    if (!exists) {
                        return reactiveRedisTemplate.opsForStream()
                                .createGroup("test2", "g2");
                    }
                    return Mono.empty(); // 如果组已存在，返回空
                })
                .then();
    }


    public Mono<Void> ensureStreamExists(String streamName) {
        // 向流中添加一条消息，以确保流存在
        return reactiveRedisTemplate.opsForStream().add(streamName, Map.of("noteId", new NoteId("1")))
                .then();
    }

    //@PostConstruct
    public void startConsuming() {
        ensureStreamExists("test2").subscribe();
        createConsumerGroupIfNotExists().subscribe();
        // 启动消费逻辑，例如：
        consumeMessages("test2", "g2", "c2")
                .subscribe(message -> {
                    // 处理接收到的消息
                    System.out.println("Received message: " + message);
                }, error -> {
                    // 处理错误
                    System.err.println("Error: " + error.getMessage());
                }, () -> {
                    // 完成时的处理
                    System.out.println("Consumption completed.");
                }).dispose();

    }


    /*public void listenToStream1() {
        // 创建一个 Flux，持续读取流中的新消息
        Consumer consumer = Consumer.from("groupName", "consumerName");
        Flux<NoteId> messageFlux = Flux.interval(Duration.ofSeconds(1)) // 定时轮询
                .flatMap(tick -> {
                            StreamOffset<String> streamOffset = StreamOffset.create("streamName", ReadOffset.lastConsumed());
                            Flux<ObjectRecord<String, NoteId>> read = redisTemplate.opsForStream().read(NoteId.class, consumer, streamOffset);
                            return read.map(ObjectRecord::getValue);
                        }
                );

        // 订阅 Flux，处理接收到的消息
        messageFlux.subscribeOn(Schedulers.boundedElastic())
                .subscribe(message -> {
                    // 处理接收到的消息
                    System.out.println("Received message: " + message);
                }, error -> {
                    // 错误处理
                    System.err.println("Error: " + error.getMessage());
                });
    }*/

    /*public void listenToStream() {
        Consumer consumer = Consumer.from("groupName", "consumerName");

        Flux<NoteId> messageFlux = Flux.create(sink -> {
            StreamOffset<String> streamOffset = StreamOffset.create("streamName", ReadOffset.lastConsumed());

            redisTemplate.opsForStream()
                    .read(NoteId.class, consumer, StreamReadOptions.empty(),streamOffset)
                    .doOnNext(message -> {
                        sink.next(message.getValue());
                        // 这里可以添加消息确认逻辑
                    })
                    .doOnError(sink::error)
                    .subscribe();
        });

        messageFlux.subscribe(message -> {
            System.out.println("Received message: " + message);
        }, error -> {
            System.err.println("Error: " + error.getMessage());
        });
    }
*/
    /*public Flux<ObjectRecord<String, NoteId>> consumeMessages1(String streamName, String groupName, String consumerName) {
        // 使用 Consumer.from() 来创建消费者，并指定消费者组和消费者名称
        Consumer consumer = Consumer.from(groupName, consumerName);

        // 通过 read() 方法消费 Redis Stream 中的数据
        return redisTemplate.opsForStream().read(NoteId.class, consumer, StreamOffset.create(streamName, ReadOffset.lastConsumed()))
                .doOnNext(record -> {
                    // 处理消费的消息
                    System.out.println("Received message: " + record.getValue());
                });
    }*/

    public Flux<NoteId> consumeMessages(String streamName, String groupName, String consumerName) {
        Consumer consumer = Consumer.from(groupName, consumerName);
        StreamOffset<Object> streamOffset = StreamOffset.create(streamName, ReadOffset.lastConsumed());
        StreamReadOptions streamReadOptions = StreamReadOptions.empty().block(Duration.ofMillis(10000)).noack().count(1);
       /* ReactiveStreamOperations<Object, Object, Object> objectObjectObjectReactiveStreamOperations = reactiveRedisTemplate.opsForStream();
        return objectObjectObjectReactiveStreamOperations.read(Object.class, consumer,streamReadOptions, streamOffset)
                .map(record -> {
                    // 处理消息，获取消息体内容
                    Object value = record.getValue();
                    System.out.println("Received message: " + value);
                    return value;
                });*/
        return reactiveRedisTemplate.opsForStream().read(consumer, streamReadOptions, streamOffset)
                .filter(Objects::nonNull)
                .flatMap(v -> {
                    System.out.println("Received message: " + v.getValue());
                    Map<Object, Object> map = v.getValue();
                    Object value = map.get("noteId");
                    if (value != null) {
                        NoteId noteId = (NoteId) value;
                        System.out.println(noteId.getId());
                        return Flux.just(noteId);
                    }
                    return Flux.empty();
                });
    }


    /*public Flux<NoteId> consumeMessages(String streamName, String groupName, String consumerName) {
        Consumer consumer = Consumer.from(groupName, consumerName);
        StreamOffset<String> streamOffset = StreamOffset.create(streamName, ReadOffset.lastConsumed());
        StreamReadOptions streamReadOptions = StreamReadOptions.empty().block(Duration.ofMillis(10000)).noack().count(1);
        // 使用 subscribe() 方法异步消费消息
        return redisTemplate.opsForStream().read(NoteId.class, consumer, streamReadOptions, streamOffset)
                .filter(Objects::nonNull) // 过滤掉 null
                .map(ObjectRecord::getValue)
                .switchIfEmpty(Flux.empty()); // 如果没有消息则返回空Flux
    }*/
//        return redisTemplate.opsForStream().read(Map.class, consumer, streamOffset)
//                .flatMap(record -> {
//                    // 处理消息
//                    // 确认消息已处理
//                    Map value = record.getValue();
//                    return Mono.just(value);
//                    //return redisTemplate.opsForStream().acknowledge(streamName, groupName, record.getId()).then(Mono.just(value));
//                });


    @SuppressWarnings("unchecked")
    /*public Flux<NoteId> consumeMessages2(String streamName, String groupName, String consumerName) {
        Consumer consumer = Consumer.from(groupName, consumerName);
        StreamOffset<String> streamOffset = StreamOffset.create(streamName, ReadOffset.lastConsumed());
        // 使用 subscribe() 方法异步消费消息
        Flux<ObjectRecord<String, NoteId>> read = redisTemplate.opsForStream().read(NoteId.class, consumer, streamOffset);
        return read.doOnNext(record -> {
            // 处理消费的消息
            System.out.println("Received message: " + record.getValue());
        }).map(ObjectRecord::getValue);
//        return redisTemplate.opsForStream().read(Map.class, consumer, streamOffset)
//                .flatMap(record -> {
//                    // 处理消息
//                    // 确认消息已处理
//                    Map value = record.getValue();
//                    return Mono.just(value);
//                    //return redisTemplate.opsForStream().acknowledge(streamName, groupName, record.getId()).then(Mono.just(value));
//                });
    }*/


    @Override
    public void onMessage(Record<String, Map<String, NoteId>> message) {

    }
}
