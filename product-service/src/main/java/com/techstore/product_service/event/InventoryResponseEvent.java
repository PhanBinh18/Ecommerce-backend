package com.techstore.product_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryResponseEvent {
    private Long orderId;
    private boolean success; // true nếu trừ kho đủ, false nếu hết hàng
    private String message;  // Lời nhắn (VD: "Thành công" hoặc "Lỗi: Hết hàng")
}
