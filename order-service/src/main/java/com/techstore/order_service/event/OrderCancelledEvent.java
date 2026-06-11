package com.techstore.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCancelledEvent {
    private Long orderId;
    private String cancelReason;
    private List<OrderItemEvent> rollbackItems;
    private boolean isTimeout;
}