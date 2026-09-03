package com.sast.crs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import jakarta.annotation.Resource;

@Configuration
public class RedisConfig {
    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 配置自定义redisTemplate
     * 值统一以 JSON 字符串存储（验证码/token/缓存均为 String），无需 default typing；
     * 注意：旧版 WRAPPER_ARRAY 类型标注格式的存量键将不可读（均为短期缓存，可自然过期或上线时清空 CRS:*）
     *
     * @return RedisTemplate
     */
    @Bean
    RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        JacksonJsonRedisSerializer<Object> jacksonSerializer = new JacksonJsonRedisSerializer<>(Object.class);
        // 设置键（key）的序列化采用StringRedisSerializer。
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // 设置值（value）的序列化采用Jackson 3 的 JacksonJsonRedisSerializer。
        redisTemplate.setValueSerializer(jacksonSerializer);
        redisTemplate.setHashValueSerializer(jacksonSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
