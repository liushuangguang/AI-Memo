package com.newtech.note.service.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


/**
 * 自定义 Redis 序列化器，用于处理复杂对象的序列化和反序列化。（fixme: 待实现，目前还不能正常工作）
 * <p>
 * 该类用于解决 Spring Data Redis 在序列化复杂对象时，可能遇到的问题。例如，当我们尝试读取数据时，
 * 可能会遇到对象反序列化错误，如 {_class=com.newtech.note.entity.pojo.NoteId@6e350f02, id=com.newtech.note.entity.pojo.NoteId@5a934457}。
 * <p>
 * 自定义序列化器使用 Jackson ObjectMapper 来正确处理 Redis 中对象的存储和读取，适用于简单和复杂的嵌套对象。
 *
 * @param <T> 要序列化和反序列化的对象类型
 */
public class CustomRedisSerializer<T> implements RedisSerializer<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public CustomRedisSerializer(Class<T> type) {
        this.objectMapper = new ObjectMapper(); // 使用 Jackson 进行序列化
        this.type = type;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        try {
            return objectMapper.writeValueAsBytes(t);
        } catch (Exception e) {
            throw new SerializationException("Could not serialize object", e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }

        try {
            // 将字节数组转换为字符串
            String data = new String(bytes, StandardCharsets.UTF_8);
            // 如果数据包含双引号，先去除外层的双引号
            if (data.startsWith("\"") && data.endsWith("\"")) {
                data = data.substring(1, data.length() - 1); // 去除双引号
            }
            // 检查数据是否为 Base64 编码
            if (isBase64Encoded(data)) {
                // 对 Base64 编码的数据进行解码
                String decodedData = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
                // 反序列化解码后的 JSON 数据
                return objectMapper.readValue(decodedData, type);
            } else {
                // 如果不是 Base64 编码，直接尝试反序列化
                return objectMapper.readValue(data, type);
            }
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize object", e);
        }
    }

    /**
     * 检查字符串是否为 Base64 编码。
     */
    public static boolean isBase64Encoded(String data) {
        // 简单的 Base64 检查逻辑
        try {
            Base64.getDecoder().decode(data);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
