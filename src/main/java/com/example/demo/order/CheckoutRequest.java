package com.example.demo.order;

import lombok.Data;

@Data
public class CheckoutRequest {
    private Long userId;
    private String receiverName;
    private String phoneNumber;
    private String shippingAddress;
    private String paymentMethod;
}