package com.techstore.cart_service.service;

import com.techstore.cart_service.event.OrderPlacedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderEventListener {

    @Autowired
    private CartService cartService;

    // Lắng nghe tại hòm thư "cart_queue"
    @RabbitListener(queues = "cart_queue")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        System.out.println("Cart Service nhận được tin: Dọn giỏ hàng cho User ID: " + event.getUserId());

        try {
            // Gọi hàm clearCart đã viết sẵn từ trước
            cartService.clearCart(event.getUserId());
            System.out.println("Đã dọn sạch giỏ hàng cho User ID: " + event.getUserId());
        } catch (Exception e) {
            System.err.println("Lỗi khi dọn giỏ hàng: " + e.getMessage());
        }
    }
}