package com.techstore.order_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponse {
    private Long id;
    private String orderCode;
    private Long userId;

    // Snapshot shipping info
    private String receiverName;
    private String phoneNumber;
    private String shippingAddress;

    private String paymentMethod;
    private String note;
    private String status;
    private BigDecimal totalPrice;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<OrderItemDto> items;
}