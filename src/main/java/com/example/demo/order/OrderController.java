package com.example.demo.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Đổi tên đường dẫn thành /checkout
    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(@RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(orderService.checkout(request));
    }
    // --- API MỚI DÀNH CHO ADMIN ---
    // Ví dụ: PUT /api/orders/1/status?newStatus=SHIPPING
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String newStatus) {
        try {
            Order updatedOrder = orderService.updateOrderStatus(id, newStatus);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            // Trả về mã 400 Bad Request kèm thông báo lỗi thay vì sập server
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}