package com.techstore.order_service.client;

import com.techstore.order_service.dto.CartDto;
import com.techstore.order_service.dto.ProductResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.techstore.order_service.config.FeignConfig;

// 1. Gọi sang Cart Service (Áp dụng cấu hình truyền Token)
@FeignClient(name = "cart-service", configuration = FeignConfig.class)
public interface CartClient {
    @GetMapping("/api/carts/my-cart")
    CartDto getMyCart();

    @DeleteMapping("/api/carts/clear")
    void clearCart();
}

