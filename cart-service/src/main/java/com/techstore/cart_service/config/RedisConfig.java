package com.techstore.cart_service.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * RedisTemplate<String, Object> configured to:
     * - use String serializer for keys and hash keys
     * - use Generic Jackson JSON serializer for values and hash values (Spring Boot 3.x Standard)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key serializer (String format)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 1. Initialize custom ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // Support Java 8 date/time types (LocalDateTime, LocalDate)
        objectMapper.registerModule(new JavaTimeModule());

        // Prevent crashing if object has unknown properties
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // [CRITICAL FOR GENERIC SERIALIZER]: Tell Jackson to store class type information in JSON
        // This replaces the old requirement of manually mapping classes
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 2. Pass the ObjectMapper directly into the constructor (Fixes the deprecated warning)
        GenericJackson2JsonRedisSerializer genericJacksonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 3. Apply serializers
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        template.setValueSerializer(genericJacksonSerializer);
        template.setHashValueSerializer(genericJacksonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}