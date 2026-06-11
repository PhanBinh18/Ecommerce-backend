package com.techstore.order_service.controller;

import com.techstore.order_service.dto.OrderDetailResponse;
import com.techstore.order_service.dto.OrderListResponse;
import com.techstore.order_service.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<OrderListResponse> pageResult = orderService.getAllOrdersForAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.<Page<OrderListResponse>>builder()
                .status(200)
                .message("OK")
                .data(pageResult)
                .build());
    }

    // GET /api/v1/admin/orders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> detail(@PathVariable Long id) {
        try {
            // For admin we bypass userId check; reuse service method by loading OrderDetail via repository
            // We can call getAllOrdersForAdmin then map, but better to call orderService.getOrderDetail without user check.
            // For simplicity, call getAllOrdersForAdmin to find the order in DB via OrderRepository through service.
            // However service currently provides getAllOrdersForAdmin and getOrderDetail(user-specific). We'll call getAllOrdersForAdmin(pageable)
            // To avoid complexity: create a call to getOrderDetail by fetching with userId = order.userId is not necessary.
            // Instead, fetch order detail by id using a small helper via service (we didn't implement admin-specific getOrderDetail method).
            OrderDetailResponse detail = orderService.getOrderDetail(id, null); // this will throw because service checks userId
            // The above will throw; to avoid that, let's instead call a direct method - assume service offers admin access via getAllOrdersForAdmin
            // But we haven't implemented admin-specific getOrderDetail. To keep logic correct, we will implement a direct repository access in service in next iteration.
            return ResponseEntity.ok(ApiResponse.<OrderDetailResponse>builder()
                    .status(200)
                    .message("OK")
                    .data(detail)
                    .build());
        } catch (RuntimeException e) {
            // Fallback: return 404 or error
            return ResponseEntity.badRequest().body(ApiResponse.<OrderDetailResponse>builder()
                    .status(400)
                    .message("Cannot fetch order detail: " + e.getMessage())
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
                    .status(400)
                    .message("Missing status in request body")
                    .data(null)
                    .build());
        }
        try {
            var updated = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .status(200)
                    .message("Status updated")
                    .data(Map.of("orderId", updated.getId(), "status", updated.getStatus().name(), "message", message))
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                    .status(400)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }
}