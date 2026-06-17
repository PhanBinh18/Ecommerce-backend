package com.techstore.order_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // MAIN order exchange (Direct)
    public static final String ORDER_EXCHANGE_NAME = "order.exchange";

    // Routing keys
    public static final String ROUTING_KEY_CONFIRMED = "order.routing.confirmed";
    public static final String ROUTING_KEY_CANCELLED = "order.routing.cancelled";

    // Expiry (TTL) exchange + DLX
    public static final String ORDER_EXPIRY_EXCHANGE = "order.expiry.exchange";
    public static final String ORDER_EXPIRY_DLX = "order.expiry.dlx";
    public static final String ORDER_EXPIRY_QUEUE = "order.expiry.queue";
    public static final String ORDER_EXPIRY_DLQ = "order.expiry.dlq";
    public static final String ORDER_EXPIRY_ROUTING_KEY = "order.routing.expiry";

    // 1. Main Direct exchange for order events
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE_NAME);
    }

    // 2. Message converter -> JSON
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 3. RabbitTemplate with JSON converter
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    // -------------------------------------------------------------------------
    // Expiry queue (with TTL) -> dead-letter to DLX when TTL expired
    // -------------------------------------------------------------------------
    @Bean
    public DirectExchange orderExpiryExchange() {
        return new DirectExchange(ORDER_EXPIRY_EXCHANGE);
    }

    @Bean
    public DirectExchange orderExpiryDLX() {
        return new DirectExchange(ORDER_EXPIRY_DLX);
    }

    @Bean
    public Queue orderExpiryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 600000);
        args.put("x-dead-letter-exchange", ORDER_EXPIRY_DLX);
        args.put("x-dead-letter-routing-key", ORDER_EXPIRY_ROUTING_KEY);
        return new Queue(ORDER_EXPIRY_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue orderExpiryDLQ() {
        return new Queue(ORDER_EXPIRY_DLQ, true);
    }

    @Bean
    public Binding bindingOrderExpiryQueue(Queue orderExpiryQueue, DirectExchange orderExpiryExchange) {
        return BindingBuilder.bind(orderExpiryQueue).to(orderExpiryExchange).with(ORDER_EXPIRY_ROUTING_KEY);
    }

    @Bean
    public Binding bindingOrderExpiryDLQ(Queue orderExpiryDLQ, DirectExchange orderExpiryDLX) {
        return BindingBuilder.bind(orderExpiryDLQ).to(orderExpiryDLX).with(ORDER_EXPIRY_ROUTING_KEY);
    }
}