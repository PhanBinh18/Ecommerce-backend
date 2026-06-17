package com.techstore.order_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    private Long addressId;
    private String paymentMethod;
    private String note;
    private String receiverName;
    private String phoneNumber;
    private String shippingAddress;
}