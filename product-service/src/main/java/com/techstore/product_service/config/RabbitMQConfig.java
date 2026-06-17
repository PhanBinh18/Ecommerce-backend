package com.techstore.product_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Main Order Exchange (Topic-based routing)
    public static final String ORDER_EXCHANGE_NAME = "order.exchange";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.routing.confirmed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.routing.cancelled";

    // Queues for Product Service (Saga Pattern)
    public static final String PRODUCT_CONFIRMED_QUEUE = "product_confirmed_queue";
    public static final String PRODUCT_CANCELLED_QUEUE = "product_cancelled_queue";

    // Reply Exchange (for responses back to Order Service)
    public static final String REPLY_EXCHANGE_NAME = "inventory_reply_exchange";

    // 1. Topic Exchange for routing by key
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE_NAME, true, false);
    }

    // 2. Queues for confirmed & cancelled orders
    @Bean
    public Queue productConfirmedQueue() {
        return new Queue(PRODUCT_CONFIRMED_QUEUE, true); // durable = true
    }

    @Bean
    public Queue productCancelledQueue() {
        return new Queue(PRODUCT_CANCELLED_QUEUE, true); // durable = true
    }

    // 3. Bindings: attach queues to exchange with routing keys
    @Bean
    public Binding bindingProductConfirmedQueue(Queue productConfirmedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(productConfirmedQueue)
                .to(orderExchange)
                .with(ORDER_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingProductCancelledQueue(Queue productCancelledQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(productCancelledQueue)
                .to(orderExchange)
                .with(ORDER_CANCELLED_ROUTING_KEY);
    }

    // 4. Message Converter (JSON)
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 5. RabbitTemplate (for sending replies)
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    // 6. Reply Exchange (Fanout for responses)
    @Bean
    public TopicExchange inventoryReplyExchange() {
        return new TopicExchange(REPLY_EXCHANGE_NAME, true, false);
    }
}