package com.techstore.order_service.controller;

import com.techstore.order_service.dto.*;
import com.techstore.order_service.service.OrderService;
import com.techstore.order_service.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // POST /api/v1/orders/checkout
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse resp = orderService.checkout(request);
            return ResponseEntity.ok(ApiResponse.<CheckoutResponse>builder()
                    .status(200)
                    .message("Checkout success")
                    .data(resp)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<CheckoutResponse>builder()
                    .status(400)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    // GET /api/v1/orders/history
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<java.util.List<OrderListResponse>>> getHistory() {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            java.util.List<OrderListResponse> list = orderService.getOrderHistory(currentUserId);
            return ResponseEntity.ok(ApiResponse.<java.util.List<OrderListResponse>>builder()
                    .status(200)
                    .message("OK")
                    .data(list)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ApiResponse.<java.util.List<OrderListResponse>>builder()
                    .status(401)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    // GET /api/v1/orders/history/{id}
    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getHistoryDetail(@PathVariable("id") Long id) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            OrderDetailResponse detail = orderService.getOrderDetail(id, currentUserId);
            return ResponseEntity.ok(ApiResponse.<OrderDetailResponse>builder()
                    .status(200)
                    .message("OK")
                    .data(detail)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<OrderDetailResponse>builder()
                    .status(400)
                    .message(e.getMessage())
                    .data(null)
                    .build());
        }
    }

    // GET /api/v1/orders/vnpay-ipn (public webhook)
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<ApiResponse<Map<String, String>>> vnpayIpn(
            @RequestParam(name = "vnp_Amount", required = false) String vnp_Amount,
            @RequestParam(name = "vnp_OrderInfo", required = false) String vnp_OrderInfo,
            @RequestParam(name = "vnp_ResponseCode", required = false) String vnp_ResponseCode,
            @RequestParam(name = "vnp_SecureHash", required = false) String vnp_SecureHash,
            @RequestParam(name = "vnp_TxnRef", required = false) String vnp_TxnRef
    ) {
        VNPayIPNRequest ipn = VNPayIPNRequest.builder()
                .vnp_Amount(vnp_Amount)
                .vnp_OrderInfo(vnp_OrderInfo)
                .vnp_ResponseCode(vnp_ResponseCode)
                .vnp_SecureHash(vnp_SecureHash)
                .vnp_TxnRef(vnp_TxnRef)
                .build();

        try {
            orderService.handleVNPayCallback(ipn);
            // VNPay expects simple response; still wrap in ApiResponse
            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .status(200)
                    .message("IPN processed")
                    .data(Map.of("result", "OK"))
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, String>>builder()
                    .status(400)
                    .message(e.getMessage())
                    .data(Map.of("result", "FAILED"))
                    .build());
        }
    }
}