package com.techstore.cart_service.dto;

import lombok.Data;

@Data
public class CartItemDto {
    private Long id;
    private Long productId;
    private Integer quantity;
    // Đổi từ class Product cũ sang DTO mới
    private ProductResponseDTO product;
}