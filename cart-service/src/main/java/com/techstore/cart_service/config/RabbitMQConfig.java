package com.techstore.cart_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "order_fanout_exchange";
    public static final String CART_QUEUE = "cart_queue"; // Hòm thư riêng cho Cart

    @Bean
    public FanoutExchange orderExchange() {
        return new FanoutExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue cartQueue() {
        return new Queue(CART_QUEUE, true);
    }

    // Nối hòm thư Cart vào chung cái Loa Phường với Product
    @Bean
    public Binding bindingCartQueue(Queue cartQueue, FanoutExchange orderExchange) {
        return BindingBuilder.bind(cartQueue).to(orderExchange);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}