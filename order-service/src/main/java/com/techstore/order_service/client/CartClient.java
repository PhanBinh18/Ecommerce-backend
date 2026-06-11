package com.techstore.order_service.client;

import com.techstore.order_service.dto.CartDto;
import com.techstore.order_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

// 1. Gọi sang Cart Service (Áp dụng cấu hình truyền Token)
@FeignClient(name = "cart-service", configuration = FeignConfig.class)
public interface CartClient {
    @GetMapping("/api/carts/my-cart")
    CartDto getMyCart();

    // public API (existing)
    @DeleteMapping("/api/carts/clear")
    void clearCart();

    // internal API for other services to clear cart after confirmed/payment
    @DeleteMapping("/api/v1/internal/carts/clear")
    void clearCartInternal();
}