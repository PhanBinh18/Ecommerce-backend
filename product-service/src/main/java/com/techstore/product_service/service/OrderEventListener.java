package com.techstore.product_service.service;


import com.techstore.product_service.event.OrderItemEvent;
import com.techstore.product_service.event.OrderPlacedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderEventListener {

    @Autowired
    private ProductService productService;

    // Lắng nghe 24/7 tại hòm thư "product_queue"
    @RabbitListener(queues = "product_queue")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        System.out.println("Product Service nhận được Đơn hàng #" + event.getOrderId());

        try {
            // Duyệt qua từng món hàng trong tờ giấy và gọi hàm trừ kho Atomic mà ta đã viết
            for (OrderItemEvent item : event.getItems()) {
                productService.reduceStock(item.getProductId(), item.getQuantity());
                System.out.println("Đã trừ kho thành công Sản phẩm ID: " + item.getProductId());
            }
        } catch (Exception e) {
            // Tạm thời in ra lỗi nếu hết hàng (Giai đoạn 2.2 ta sẽ xử lý việc báo lại cho Order sau)
            System.err.println("Lỗi trừ kho cho Đơn hàng #" + event.getOrderId() + ": " + e.getMessage());
        }
    }
}
