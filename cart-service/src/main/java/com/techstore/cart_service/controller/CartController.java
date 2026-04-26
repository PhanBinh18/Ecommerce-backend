package com.techstore.cart_service.controller;


import com.techstore.cart_service.dto.CartDto;
import com.techstore.cart_service.dto.CartRequest;
import com.techstore.cart_service.security.SecurityUtils;
import com.techstore.cart_service.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    @Autowired
    private CartService cartService;

    // 2. XÓA {userId} trên URL, tự móc ID từ SecurityUtils
    @GetMapping("/my-cart")
    public ResponseEntity<CartDto> getCart() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(cartService.getCartByUserId(currentUserId));
    }

    // 3. Truyền thêm ID vào hàm addToCart của Service
    @PostMapping("/add")
    public ResponseEntity<CartDto> addToCart(@RequestBody CartRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(cartService.addToCart(currentUserId, request));
    }

    // 4. XÓA {userId} trên URL, tự móc ID từ SecurityUtils
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        cartService.clearCart(currentUserId);
        return ResponseEntity.ok("Đã làm trống giỏ hàng thành công!");
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @PathVariable Long itemId,
            @RequestBody java.util.Map<String, Integer> payload) {
        int quantity = payload.get("quantity");
        return ResponseEntity.ok(cartService.updateItemQuantity(itemId, quantity));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<String> removeItem(@PathVariable Long itemId) {
        cartService.removeItem(itemId);
        return ResponseEntity.ok("Đã xóa sản phẩm khỏi giỏ hàng!");
    }
}
