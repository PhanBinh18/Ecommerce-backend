package com.techstore.cart_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponse {
    private List<CartItemDto> items;
    private BigDecimal totalAmount;
}