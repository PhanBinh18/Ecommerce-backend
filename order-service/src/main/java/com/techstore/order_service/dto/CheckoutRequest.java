package com.techstore.order_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    private Long addressId;       // id tham chiếu Address Service
    private String paymentMethod; // COD / VNPAY / ...
    private String note;          // Ghi chú đơn hàng
    private String receiverName;
    private String phoneNumber;
    private String shippingAddress;
}