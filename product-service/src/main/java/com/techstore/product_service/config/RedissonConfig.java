package com.techstore.product_service.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    /**
     * Create RedissonClient bean to manage RLock and other distributed objects.
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        String redisUrl = "redis://" + redisHost + ":" + redisPort;
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer()
                    .setAddress(redisUrl)
                    .setPassword(redisPassword)
                    .setConnectionPoolSize(32)
                    .setConnectionMinimumIdleSize(8)
                    .setTimeout(3000);
        } else {
            config.useSingleServer()
                    .setAddress(redisUrl)
                    .setConnectionPoolSize(32)
                    .setConnectionMinimumIdleSize(8)
                    .setTimeout(3000);
        }

        return Redisson.create(config);
    }
}