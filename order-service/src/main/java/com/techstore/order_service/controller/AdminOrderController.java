package com.techstore.order_service.controller;

import com.techstore.order_service.dto.OrderDetailResponse;
import com.techstore.order_service.dto.OrderListResponse;
import com.techstore.order_service.dto.ApiResponse;
import com.techstore.order_service.dto.OrderPageResponse;
import com.techstore.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    // GET /api/v1/admin/orders?page=0&size=10
    @GetMapping
    public ResponseEntity<ApiResponse<OrderPageResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        OrderPageResponse pageResult = orderService.getAllOrdersForAdmin(pageable, status);

        return ResponseEntity.ok(ApiResponse.<OrderPageResponse>builder()
                .status("SUCCESS")
                .message("Lấy danh sách đơn hàng thành công")
                .data(pageResult)
                .build());
    }

    // GET /api/v1/admin/orders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> detail(@PathVariable Long id) {
        try {
            OrderDetailResponse detail = orderService.getOrderDetail(id, null);

            return ResponseEntity.ok(ApiResponse.<OrderDetailResponse>builder()
                    .status("SUCCESS")
                    .message("Lấy chi tiết đơn hàng thành công")
                    .data(detail)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<OrderDetailResponse>builder()
                    .status("ERROR")
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    // PATCH /api/v1/admin/orders/{id}/status
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        String message = body.getOrDefault("message", "");

        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .status("ERROR")
                    .message("Thiếu trạng thái (status) trong request body")
                    .data(null)
                    .build());
        }

        try {
            var updated = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .status("SUCCESS")
                    .message("Cập nhật trạng thái thành công")
                    .data(Map.of("orderId", updated.getId(), "status", updated.getStatus().name(), "message", message))
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .status("ERROR")
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }
}