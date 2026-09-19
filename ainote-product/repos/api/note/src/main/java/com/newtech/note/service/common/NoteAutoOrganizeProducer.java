package com.newtech.note.service.common;

import com.newtech.note.entity.pojo.NoteId;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.newtech.note.common.constant.RedisConstant.STREAM_NAME;

@Service
public class NoteAutoOrganizeProducer {
    private final ReactiveStringRedisTemplate redisTemplate;


    public NoteAutoOrganizeProducer(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<String> sendMessageToStream(NoteId noteId) {
        //通过ObjectRecord将NoteId对象写入Redis Stream中, 会出现序列化问题，可能需要自定义序列化器，但是在add方法中会进行多次序列化的操作，
        // redis stream reactive 底层对序列化和反序列化的处理有点问题，无论如何设置序列化器，都会导致消费消息失败（反序列化总是失败）
        return redisTemplate.opsForStream().add(ObjectRecord.create(STREAM_NAME, noteId))
                .map(record -> "Message sent with ID: " + record.getValue());
    }
}
