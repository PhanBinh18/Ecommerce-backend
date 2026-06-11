package com.techstore.order_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderListResponse {
    private Long id;
    private String orderCode;
    private String firstProductImage;
    private String firstProductName;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
}