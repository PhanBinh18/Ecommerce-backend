package com.techstore.order_service.controller;

import com.techstore.order_service.dto.CheckoutRequest;
import com.techstore.order_service.dto.OrderDto;
import com.techstore.order_service.entity.Order;
import com.techstore.order_service.security.SecurityUtils;
import com.techstore.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/validate-cart")
    public ResponseEntity<?> validateCartBeforeCheckout() {
        try {
            orderService.validateCartBeforeCheckout();
            // Trả về true nếu kho vẫn đủ cho tất cả các món
            return ResponseEntity.ok(Collections.singletonMap("valid", true));
        } catch (Exception e) {
            // Trả về HTTP 400 kèm câu thông báo lỗi chi tiết
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(@RequestBody CheckoutRequest request) {
        // 2. Tự động lấy ID người dùng từ Token
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 3. Truyền ID xuống Service
        return ResponseEntity.ok(orderService.checkout(currentUserId, request));
    }
    // --- API MỚI DÀNH CHO USER ---
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderDto>> getMyOrders() {
        // 1. Tự động lấy ID người dùng từ Token (Bảo mật 100%, chống IDOR)
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 2. Lấy danh sách đơn hàng và trả về
        List<OrderDto> myOrders = orderService.getMyOrders(currentUserId);
        return ResponseEntity.ok(myOrders);
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