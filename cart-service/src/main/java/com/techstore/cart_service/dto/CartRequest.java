package com.techstore.cart_service.dto;

import lombok.Data;

@Data
public class CartRequest {
    private Long productId;
    private Integer quantity;
}