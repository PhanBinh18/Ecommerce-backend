package com.ecommerce.backend.order.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String receiverName;
    private String phoneNumber;
    private String shippingAddress;
    private String paymentMethod;
}