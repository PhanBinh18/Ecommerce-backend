package com.techstore.product_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {
    private Long orderId;
    private List<OrderItemEvent> items;
    private String reason;        // e.g., "PAYMENT_FAILED", "USER_CANCELLED", "TIMEOUT"
    private Boolean isTimeout;    // true = timeout (hủy do hết hạn), false = hủy sau khi confirm
}