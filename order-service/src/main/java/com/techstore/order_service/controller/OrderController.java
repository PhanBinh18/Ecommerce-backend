package com.techstore.order_service.controller;

import com.techstore.order_service.client.ProductClient;
import com.techstore.order_service.dto.*;
import com.techstore.order_service.service.OrderService;
import com.techstore.order_service.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    @Autowired
    private OrderService orderService;

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
                    .status("SUCCESS")
                    .message("IPN processed")
                    .data(Map.of("result", "OK"))
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, String>>builder()
                    .status("ERROR")
                    .message(e.getMessage())
                    .data(Map.of("result", "FAILED"))
                    .build());
        }
    }

    // POST /api/v1/orders/{id}/cancel
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelOrder(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", defaultValue = "Người dùng đổi ý không mua nữa") String reason) {
        try {
            // Lấy ID của user đang đăng nhập để đảm bảo họ chỉ hủy được đơn của chính mình
            Long currentUserId = SecurityUtils.getCurrentUserId();

            // Gọi hàm xử lý hủy đơn trong OrderService
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
//
//    // ==========================================
//    // ADMIN ENDPOINTS
//    // ==========================================
//
//    // Lấy toàn bộ danh sách đơn hàng (Có thể lọc theo trạng thái)
//    @GetMapping("/admin/orders")
//    public ResponseEntity<ApiResponse<java.util.List<OrderListResponse>>> getAllOrdersForAdmin(
//            @RequestParam(required = false) String status) {
//        // Bạn cần viết thêm hàm getAllOrdersForAdmin(status) trong OrderService
//        java.util.List<OrderListResponse> list = orderService.getAllOrdersForAdmin(status);
//        return ResponseEntity.ok(ApiResponse.<java.util.List<OrderListResponse>>builder()
//                .status("SUCCESS").message("Lấy danh sách đơn hàng thành công").data(list).build());
//    }
//
//    // Xem chi tiết đơn hàng (Admin xem được của mọi User)
//    @GetMapping("/admin/orders/{id}")
//    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetailForAdmin(@PathVariable("id") Long id) {
//        // Gọi hàm getOrderDetail hiện tại nhưng truyền tham số để bypass check UserID,
//        // hoặc viết hàm riêng getOrderDetailForAdmin(id)
//        OrderDetailResponse detail = orderService.getOrderDetailForAdmin(id);
//        return ResponseEntity.ok(ApiResponse.<OrderDetailResponse>builder()
//                .status("SUCCESS").message("Chi tiết đơn hàng").data(detail).build());
//    }
//
//    // Cập nhật trạng thái đơn hàng (Dùng hàm updateOrderStatus bạn đã có)
//    @PutMapping("/admin/orders/{id}/status")
//    public ResponseEntity<ApiResponse<String>> updateOrderStatusForAdmin(
//            @PathVariable("id") Long id,
//            @RequestParam("status") String status) {
//        try {
//            orderService.updateOrderStatus(id, status);
//            return ResponseEntity.ok(ApiResponse.<String>builder()
//                    .status("SUCCESS").message("Cập nhật trạng thái thành công").data(status).build());
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
//                    .status("ERROR").message(e.getMessage()).data(null).build());
//        }
//    }
}