package com.example.demo.cart;

import com.example.demo.product.Product;
import com.example.demo.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductService productService; // Giao tiếp liên module

    @Autowired
    private CartItemRepository cartItemRepository;

    // Lấy giỏ hàng (Nếu user chưa có thì tự động tạo giỏ trống)
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public Cart addToCart(CartRequest request) {
        // 1. Kiểm tra tồn kho từ module Product (Chỉ check, chưa trừ kho)
        Product product = productService.getProductById(request.getProductId());
        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Vượt quá số lượng tồn kho của: " + product.getName());
        }

        // 2. Lấy giỏ hàng của User
        Cart cart = getCartByUserId(request.getUserId());

        // 3. Kiểm tra xem sản phẩm này đã có trong giỏ chưa
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Có rồi thì cộng dồn số lượng
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            // Chưa có thì tạo item mới
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    // Làm trống giỏ hàng (Sẽ dùng khi Order thành công)
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().clear(); // orphanRemoval sẽ tự lo việc xóa dưới DB
        cartRepository.save(cart);
    }

    // Hàm cập nhật số lượng
    @Transactional
    public Cart updateItemQuantity(Long itemId, int quantity) {
        // Tìm chi tiết giỏ hàng theo ID
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm này trong giỏ!"));

        // Cập nhật số lượng mới
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        // Trả về lại toàn bộ Giỏ hàng (chứa số liệu mới nhất) để Frontend render lại
        return cartItem.getCart();
    }

    // Hàm xóa 1 món đồ
    @Transactional
    public void removeItem(Long itemId) {
        if (!cartItemRepository.existsById(itemId)) {
            throw new RuntimeException("Không tìm thấy sản phẩm này trong giỏ!");
        }
        cartItemRepository.deleteById(itemId);
    }
}