package com.newtech.note.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.entity.pojo.NoteId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfiguration {

    /**
     * 对于Redis String类型的数据，使用RedisReactiveAutoConfiguration中的StringReactiveRedisTemplate，
     * 对于其他类型的数据，使用Jackson2JsonRedisSerializer序列化器，并使用ReactiveRedisTemplate进行序列化。
     *
     * @param factory   ReactiveRedisConnectionFactory
     * @param valueType Class<T>
     * @return ReactiveRedisTemplate
     */
    public <T> ReactiveRedisTemplate<String, T> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory, Class<T> valueType) {
        // 配置 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 创建 Jackson2JsonRedisSerializer
        Jackson2JsonRedisSerializer<T> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, valueType);

        // 使用 StringRedisSerializer 序列化 key，使用 Jackson2JsonRedisSerializer 序列化 value
        RedisSerializationContext<String, T> context = RedisSerializationContext.<String, T>newSerializationContext()
                .key(StringRedisSerializer.UTF_8)
                .value(jackson2JsonRedisSerializer)
                .hashKey(StringRedisSerializer.UTF_8)
                .hashValue(jackson2JsonRedisSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    /**
     * 在没实现自定义的序列化器之前，禁止使用RedisReactiveAutoConfiguration中的NoteIdReactiveRedisTemplate，
     * 并且在使用前，需要将序列化器替换为自定义的序列化器 {@link com.newtech.note.service.common.CustomRedisSerializer}。
     * <code>
     * Jackson2JsonRedisSerializer<NoteId> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(NoteId.class);
     * objectMapper.registerModule(new SimpleModule().addSerializer(NoteId.class, new CustomRedisSerializer()));
     * objectMapper.registerModule(new SimpleModule().addDeserializer(NoteId.class, new CustomRedisDeserializer()));
     * </code>
     *
     * @param factory ReactiveRedisConnectionFactory
     * @return ReactiveRedisTemplate<String, NoteId>
     */
    @Bean
    @Deprecated(forRemoval = true)
    public ReactiveRedisTemplate<String, NoteId> noteIdReactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        return reactiveRedisTemplate(factory, NoteId.class);
    }
}
