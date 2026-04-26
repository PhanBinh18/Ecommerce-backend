package com.techstore.cart_service.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartDto {
    private Long id;
    private Long userId;
    private List<CartItemDto> items;
    private LocalDateTime updatedAt;
}