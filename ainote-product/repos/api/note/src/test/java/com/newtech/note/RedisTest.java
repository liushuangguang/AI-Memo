package com.newtech.note;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.entity.pojo.NoteId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest // 表示这是一个专注于 Redis 相关操作的测试
public class RedisTest {

    @Autowired
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Autowired
    @Qualifier("noteIdReactiveRedisTemplate")
    private ReactiveRedisTemplate<String, NoteId> noteIdReactiveRedisTemplate;

    private static final String testKey = "testKey";
    private static final String testValue = "Hello, Redis!";
    private static final String streamKey = "mystream2";
    private static final String STREAM_KEY = "testStream2";

    /**
     * 每次测试前，清除数据
     */
    //@BeforeEach
    void setUp() {
        //这里必须用block()方法，因为RedisTemplate的操作都是异步的，所以需要等待结果返回，如果用subscribe()方法，则会立即返回一个Disposable对象，而不会等待结果返回
        //这样会导致后续的测试结果出问题
        reactiveStringRedisTemplate.delete(testKey).block();
        noteIdReactiveRedisTemplate.delete(streamKey).block();
        noteIdReactiveRedisTemplate.delete(STREAM_KEY).block();
    }

    //@Test
    void testSetAndGet() {
        // 将一个键值对设置到 Redis 中
        StepVerifier.create(reactiveStringRedisTemplate.opsForValue().set(testKey, testValue))
                .expectNext(true)
                .verifyComplete();
        // 从 Redis 中读取刚刚设置的键值对
        StepVerifier.create(reactiveStringRedisTemplate.opsForValue().get(testKey))
                .expectNext(testValue)
                .verifyComplete();
    }

    //@Test
    void testDelete() {
        // 设置一个键值对
        reactiveStringRedisTemplate.opsForValue().set(testKey, testValue).block();
        // 删除这个键
        StepVerifier.create(reactiveStringRedisTemplate.delete(testKey))
                .expectNext(1L)
                .verifyComplete();
        // 验证键已经不存在
        StepVerifier.create(reactiveStringRedisTemplate.hasKey(testKey))
                .expectNext(false)
                .verifyComplete();
    }

    //@Test
    void testIncrement() {
        String numberKey = "numberKey";
        // 设置初始值为 1
        reactiveStringRedisTemplate.opsForValue().set(numberKey, "1").block();
        // 自增
        StepVerifier.create(reactiveStringRedisTemplate.opsForValue().increment(numberKey))
                .expectNext(2L)
                .verifyComplete();
    }

    //@Test
    void testStreamOperations() {
        // 准备待添加到 Stream 的数据
        Map<String, String> fields = new HashMap<>();
        fields.put("id", "123");
        // 使用 XADD 添加数据到 Stream
        Mono<RecordId> addRecordMono = reactiveStringRedisTemplate
                .opsForStream()
                .add(MapRecord.create(streamKey, fields));

        StepVerifier.create(addRecordMono)
                .expectNextCount(1)  // 检查是否成功添加了一条记录
                .verifyComplete();
        // 使用 XRANGE 读取 Stream 数据
        StepVerifier.create(reactiveStringRedisTemplate
                        .opsForStream()
                        .range(streamKey, org.springframework.data.domain.Range.unbounded()))
                .expectNextMatches(record -> {
                    System.out.println("Stream Record: " + record);
                    Map<Object, Object> value = record.getValue();
                    ObjectMapper mapper = new ObjectMapper();
                    NoteId noteId = mapper.convertValue(value, NoteId.class);
                    return noteId.getId().equals("123");
                })
                .verifyComplete();
    }

    //@Test
    void testStreamAddAndReadObjectRecord() {
        // 使用 XADD 添加数据到 Stream
        Mono<RecordId> addRecordMono = reactiveStringRedisTemplate
                .opsForStream()
                .add(ObjectRecord.create(streamKey, new NoteId("123")));
        StepVerifier.create(addRecordMono)
                .expectNextCount(1)  // 检查是否成功添加了一条记录
                .verifyComplete();
        // 使用 XRANGE 读取 Stream 数据
        StepVerifier.create(reactiveStringRedisTemplate
                        .opsForStream()
                        .range(streamKey, org.springframework.data.domain.Range.unbounded()))
                .expectNextMatches(record -> {
                    System.out.println("Stream Record: " + record);
                    Map<Object, Object> value = record.getValue();
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    //{_class=com.newtech.note.entity.pojo.NoteId, id=123}
                    NoteId noteId = objectMapper.convertValue(value, NoteId.class);
                    return noteId.getId().equals("123");
                })
                .verifyComplete();
    }


    //@Test
    public void testStreamAddAndReadObjectRecordByCustomRedisTemplate() {
        // 创建 MyObject 实例
        NoteId myObject = new NoteId("1");
        // 获取 Stream 操作接口
        ReactiveStreamOperations<String, String, NoteId> streamOps = noteIdReactiveRedisTemplate.opsForStream();
        // 向 Stream 中添加对象
        Mono<RecordId> addResult = streamOps.add(ObjectRecord.create(STREAM_KEY, myObject));
        StepVerifier.create(addResult)
                .expectNextMatches(recordId -> recordId != null && !recordId.getValue().isBlank())
                .verifyComplete();
        // 从 Stream 中读取对象
        Mono<MapRecord<String, String, NoteId>> readResult = streamOps.range(STREAM_KEY, Range.unbounded()).next();
        StepVerifier.create(readResult)
                .expectNextMatches(record -> {
                    Map<String, NoteId> value = record.getValue();
                    System.out.println("map is : " + value);
                    return true;
                })
                .verifyComplete();
    }
}
