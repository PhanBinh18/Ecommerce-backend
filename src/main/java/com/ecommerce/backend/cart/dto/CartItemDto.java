package com.ecommerce.backend.cart.dto;

import com.ecommerce.backend.product.entity.Product;
import lombok.Data;

@Data
public class CartItemDto {
    private Long id;
    private Long productId;
    private Integer quantity;
    // Đây là nơi chứa cục Product xịn, không bị Hibernate quản lý
    private Product product;
}