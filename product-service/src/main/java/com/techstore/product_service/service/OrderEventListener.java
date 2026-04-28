package com.techstore.product_service.service;

import com.techstore.product_service.config.RabbitMQConfig;
import com.techstore.product_service.event.InventoryResponseEvent;
import com.techstore.product_service.event.OrderItemEvent;
import com.techstore.product_service.event.OrderPlacedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderEventListener {

    @Autowired
    private ProductService productService;

    @Autowired
    private RabbitTemplate rabbitTemplate; // Công cụ để trả lời

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_QUEUE)
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        System.out.println("📬 Product Service đang xử lý Đơn hàng #" + event.getOrderId());

        try {
            // 1. Cố gắng trừ kho
            for (OrderItemEvent item : event.getItems()) {
                productService.reduceStock(item.getProductId(), item.getQuantity());
            }

            // 2. Nếu trừ kho chót lọt -> Gửi báo cáo THÀNH CÔNG
            InventoryResponseEvent successResponse = new InventoryResponseEvent(event.getOrderId(), true, "Trừ kho thành công");
            rabbitTemplate.convertAndSend(RabbitMQConfig.REPLY_EXCHANGE_NAME, "", successResponse);
            System.out.println("Đã trừ kho và báo cáo THÀNH CÔNG cho Đơn #" + event.getOrderId());

        } catch (Exception e) {
            // 3. Nếu gặp lỗi (hết hàng) -> Gửi báo cáo THẤT BẠI
            InventoryResponseEvent failResponse = new InventoryResponseEvent(event.getOrderId(), false, e.getMessage());
            rabbitTemplate.convertAndSend(RabbitMQConfig.REPLY_EXCHANGE_NAME, "", failResponse);
            System.err.println("Kho không đủ! Đã báo cáo THẤT BẠI cho Đơn #" + event.getOrderId());
        }
    }
}
