package com.ecommerce.backend.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    private Long id;
    private String receiverName;
    private String phoneNumber;
    private String shippingAddress;
    private String paymentMethod;
    private String status;
    private BigDecimal totalPrice;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
}