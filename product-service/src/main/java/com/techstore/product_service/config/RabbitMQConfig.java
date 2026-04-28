package com.techstore.product_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "order_fanout_exchange";
    public static final String PRODUCT_QUEUE = "product_queue"; // Tên hòm thư của Product

    // 1. Khai báo lại tên Loa Phường (để biết đường mà nối)
    @Bean
    public FanoutExchange orderExchange() {
        return new FanoutExchange(EXCHANGE_NAME);
    }

    // 2. Tạo Hòm thư riêng cho Product Service
    @Bean
    public Queue productQueue() {
        // Tham số 'true' nghĩa là hòm thư này bền vững (durable), tắt server bật lại không bị mất
        return new Queue(PRODUCT_QUEUE, true);
    }

    // 3. Xây đường ống nối Hòm thư vào Loa phường
    @Bean
    public Binding bindingProductQueue(Queue productQueue, FanoutExchange orderExchange) {
        return BindingBuilder.bind(productQueue).to(orderExchange);
    }

    // 4. Bộ dịch thuật JSON (Nhận JSON biến thành Object Java)
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
