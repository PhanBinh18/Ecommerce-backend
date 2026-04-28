package com.techstore.order_service.service;

import com.techstore.order_service.config.RabbitMQConfig;
import com.techstore.order_service.event.InventoryResponseEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventListener {

    @Autowired
    private OrderService orderService;

    // Trực 24/7 ở hòm thư báo cáo
    @RabbitListener(queues = RabbitMQConfig.ORDER_REPLY_QUEUE)
    public void handleInventoryResponse(InventoryResponseEvent event) {
        if (event.isSuccess()) {
            System.out.println("Đơn #" + event.getOrderId() + " đủ hàng. Đang chốt đơn (CONFIRMED)!");
            // Gọi hàm update đã có sẵn của bạn
            orderService.updateOrderStatus(event.getOrderId(), "CONFIRMED");
        } else {
            System.err.println("Đơn #" + event.getOrderId() + " hết hàng. Đang Hủy đơn (CANCELLED)! Lý do: " + event.getMessage());
            // Lùi lại giao dịch bằng cách hủy đơn
            orderService.updateOrderStatus(event.getOrderId(), "CANCELLED");

            // Tương lai: Ở đây bạn sẽ viết thêm code gọi API hoàn tiền qua VNPay/Momo
        }
    }
}