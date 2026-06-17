package com.techstore.order_service.client;

import com.techstore.order_service.config.FeignConfig;
import com.techstore.order_service.dto.ApiResponse;
import com.techstore.order_service.dto.CartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cart-service", configuration = FeignConfig.class)
public interface CartClient {

    @GetMapping("/api/v1/carts/")
    ApiResponse<CartResponse> getMyCart();

    @DeleteMapping("/api/v1/carts/clear")
    ApiResponse<Void> clearCart();

    @DeleteMapping("/api/v1/internal/carts/clear")
    ApiResponse<Void> clearCartInternal(@RequestParam("userId") Long userId);
}