package com.techstore.order_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponse {
    private Long orderId;
    private String orderCode;
    private String paymentUrl; // null nếu không phải VNPay
}