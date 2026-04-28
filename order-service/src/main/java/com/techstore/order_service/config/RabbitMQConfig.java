package com.techstore.order_service.config;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Khai báo tên
    public static final String EXCHANGE_NAME = "order_fanout_exchange";

    // 1. Tạo Exchange dạng Fanout
    @Bean
    public FanoutExchange orderExchange() {
        return new FanoutExchange(EXCHANGE_NAME);
    }

    // 2. Cấu hình bộ dịch thuật: Biến Java Object thành JSON
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // 3. Lắp bộ dịch thuật vào công cụ gửi tin (RabbitTemplate)
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}