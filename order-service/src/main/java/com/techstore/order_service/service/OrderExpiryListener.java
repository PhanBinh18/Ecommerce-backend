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
            // Truyền null cho tham số currentUserId để đánh dấu là Hệ thống tự hủy
            orderService.cancelOrder(orderId, null, "TIMEOUT");
            log.info("Successfully cancelled expired order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to cancel expired order {}: {}", orderId, e.getMessage(), e);
        }
    }
}