package com.techstore.order_service.entity;

public enum OrderStatus {
    PENDING_UNPAID,
    PROCESSING,
    SHIPPING,
    COMPLETED,
    CANCELLED
}