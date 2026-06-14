package com.techstore.order_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderConfirmedEvent {
    private Long orderId;
    private List<OrderItemEvent> items;
}