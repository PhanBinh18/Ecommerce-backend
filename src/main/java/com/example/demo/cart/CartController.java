package com.example.demo.cart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    @Autowired
    private CartService cartService;

    // Xem giỏ hàng của 1 user
    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    // Thêm vào giỏ
    @PostMapping("/add")
    public ResponseEntity<Cart> addToCart(@RequestBody CartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(request));
    }

    // Xóa trắng giỏ hàng
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<String> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Đã làm trống giỏ hàng thành công!");
    }

    // 1. Cập nhật số lượng của 1 sản phẩm cụ thể trong giỏ
    @PutMapping("/items/{itemId}")
    public ResponseEntity<Cart> updateItemQuantity(
            @PathVariable Long itemId,
            @RequestBody java.util.Map<String, Integer> payload) {

        int quantity = payload.get("quantity");
        return ResponseEntity.ok(cartService.updateItemQuantity(itemId, quantity));
    }

    // 2. Xóa 1 sản phẩm cụ thể khỏi giỏ
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<String> removeItem(@PathVariable Long itemId) {
        cartService.removeItem(itemId);
        return ResponseEntity.ok("Đã xóa sản phẩm khỏi giỏ hàng!");
    }
}