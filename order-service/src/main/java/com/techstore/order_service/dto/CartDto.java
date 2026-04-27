package com.techstore.order_service.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartDto {
    private List<CartItemDto> items;
}