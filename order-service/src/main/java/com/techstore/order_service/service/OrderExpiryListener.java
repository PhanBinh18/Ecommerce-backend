package com.techstore.order_service.service;

import com.techstore.order_service.config.RabbitMQConfig;
import com.techstore.order_service.event.OrderExpiryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderExpiryListener {

    @Autowired
    private OrderService orderService;

    // Listen to Dead Letter Queue for expired orders
    @RabbitListener(queues = RabbitMQConfig.ORDER_EXPIRY_DLQ)
    public void handleOrderExpiry(OrderExpiryEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received null or invalid OrderExpiryEvent: {}", event);
            return;
        }
        Long orderId = event.getOrderId();
        log.info("Received OrderExpiryEvent for orderId={}", orderId);
        try {
            // Attempt to cancel order due to timeout
            orderService.cancelOrder(orderId, "TIMEOUT");
            log.info("Successfully cancelled expired order {}", orderId);
        } catch (Exception e) {
            // Log and swallow to avoid message being requeued indefinitely
            log.error("Failed to cancel expired order {}: {}", orderId, e.getMessage(), e);
        }
    }
}