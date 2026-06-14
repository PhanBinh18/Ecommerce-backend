package com.techstore.cart_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDetailResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private String thumbnail;
}