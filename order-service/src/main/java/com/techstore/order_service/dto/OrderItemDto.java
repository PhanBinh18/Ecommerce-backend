package com.techstore.order_service.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {
    private Long productId;
    private String productName;
    private String productImage;   // snapshot ảnh sản phẩm
    private BigDecimal price;      // Giá tại thời điểm mua
    private Integer quantity;
    private BigDecimal subTotal;   // price * quantity
}