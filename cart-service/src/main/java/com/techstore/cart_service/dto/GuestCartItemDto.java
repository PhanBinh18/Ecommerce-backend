package com.techstore.cart_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GuestCartItemDto {
    private Long productId;
    private String productName;
    private String thumbnailUrl;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subTotal;
}