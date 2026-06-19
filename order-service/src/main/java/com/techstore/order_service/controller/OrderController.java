package com.techstore.order_service.controller;

import com.techstore.order_service.client.ProductClient;
import com.techstore.order_service.dto.*;
import com.techstore.order_service.service.OrderService;
import com.techstore.order_service.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    @Autowired
    private OrderService orderService;

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    // POST /api/v1/orders/checkout
    @PostMapping("/orders/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse resp = orderService.checkout(request);
            return ResponseEntity.ok(ApiResponse.<CheckoutResponse>builder()
                    .status("SUCCESS")
                    .message("Checkout success")
                    .data(resp)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<CheckoutResponse>builder()
                    .status("ERROR")
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    // GET /api/v1/orders/history
    @GetMapping("/orders/history")
    public ResponseEntity<ApiResponse<java.util.List<OrderListResponse>>> getHistory() {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            java.util.List<OrderListResponse> list = orderService.getOrderHistory(currentUserId);
            return ResponseEntity.ok(ApiResponse.<java.util.List<OrderListResponse>>builder()
                    .status("SUCCESS")
                    .message("OK")
                    .data(list)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ApiResponse.<java.util.List<OrderListResponse>>builder()
                    .status("FAILED")
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    // GET /api/v1/orders/history/{id}
    @GetMapping("/orders/history/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getHistoryDetail(@PathVariable("id") Long id) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            OrderDetailResponse detail = orderService.getOrderDetail(id, currentUserId);
            return ResponseEntity.ok(ApiResponse.<OrderDetailResponse>builder()
                    .status("SUCCESS")
                    .message("OK")
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

    // GET /api/v1/orders/vnpay-ipn (public webhook)
    @GetMapping("/orders/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        Map<String, String> response = new HashMap<>();

        try {
            // Ném toàn bộ cục dữ liệu params xuống cho Service xử lý băm chữ ký và nghiệp vụ
            orderService.handleVNPayCallback(params);

            // Giao dịch thành công, lưu DB thành công -> Trả đúng format VNPay cần
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Lỗi 97: Sai chữ ký (Invalid Checksum) - Phát hiện giả mạo
            log.error("VNPay IPN Checksum failed: {}", e.getMessage());
            response.put("RspCode", "97");
            response.put("Message", "Invalid Checksum");
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Lỗi 02: Giao dịch đã được xử lý rồi (Idempotency)
            log.warn("VNPay IPN already processed: {}", e.getMessage());
            response.put("RspCode", "02");
            response.put("Message", "Order already confirmed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Lỗi 99: Các lỗi hệ thống khác không xác định (ví dụ: mất kết nối DB)
            log.error("VNPay IPN System Error: {}", e.getMessage());
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
            return ResponseEntity.ok(response);
        }
    }

    // POST /api/v1/orders/{id}/cancel
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelOrder(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", defaultValue = "Người dùng đổi ý không mua nữa") String reason) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();

            orderService.cancelOrder(id, currentUserId, reason);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .status("SUCCESS")
                    .message("Hủy đơn hàng thành công")
                    .data("Đã hủy đơn hàng ID: " + id)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .status("ERROR")
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }
}