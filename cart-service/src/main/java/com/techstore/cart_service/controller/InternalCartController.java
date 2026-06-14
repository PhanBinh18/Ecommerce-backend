package com.techstore.cart_service.controller;

import com.techstore.cart_service.dto.ApiResponse;
import com.techstore.cart_service.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints for other services (Order Service) to operate on carts.
 * These endpoints are permitted at service-level; API Gateway / network should enforce auth in production.
 */
@RestController
@RequestMapping("/api/v1/internal/carts")
public class InternalCartController {

    @Autowired
    private CartService cartService;

    /**
     * DELETE /api/v1/internal/carts/clear?userId={userId}
     * Clears the cart for the given user (DB).
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearUserCart(@RequestParam Long userId) {
        try {
            cartService.clearUserCart(userId);
            return ResponseEntity.ok(new ApiResponse<>("success", "User cart cleared", "OK"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse<>("error", e.getMessage(), null));
        }
    }
}