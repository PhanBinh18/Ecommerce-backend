package com.techstore.order_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
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

    public static final String REPLY_EXCHANGE_NAME = "inventory_reply_exchange";
    public static final String ORDER_REPLY_QUEUE = "order_reply_queue";

    // 1. Khai báo cái Loa phường mà Product dùng để báo cáo
    @Bean
    public FanoutExchange inventoryReplyExchange() {
        return new FanoutExchange(REPLY_EXCHANGE_NAME);
    }

    // 2. Tạo Hòm thư cho Order để hứng kết quả
    @Bean
    public Queue orderReplyQueue() {
        return new Queue(ORDER_REPLY_QUEUE, true);
    }

    // 3. Nối Hòm thư vào Loa phường
    @Bean
    public Binding bindingOrderReplyQueue(Queue orderReplyQueue, FanoutExchange inventoryReplyExchange) {
        return BindingBuilder.bind(orderReplyQueue).to(inventoryReplyExchange);
    }
}