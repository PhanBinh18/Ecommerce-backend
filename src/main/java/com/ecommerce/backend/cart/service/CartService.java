package com.ecommerce.backend.cart.service;

import com.ecommerce.backend.cart.dto.CartDto;
import com.ecommerce.backend.cart.dto.CartItemDto;
import com.ecommerce.backend.cart.dto.CartRequest;
import com.ecommerce.backend.cart.entity.Cart;
import com.ecommerce.backend.cart.entity.CartItem;
import com.ecommerce.backend.cart.repository.CartItemRepository;
import com.ecommerce.backend.cart.repository.CartRepository;
import com.ecommerce.backend.product.entity.Product;
import com.ecommerce.backend.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartItemRepository cartItemRepository;

    // =================================================================
    // HÀM BỔ TRỢ: Chuyển đổi từ Cart (Entity) sang CartDto (Gửi về React)
    // =================================================================
    private CartDto mapToDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUserId());
        dto.setUpdatedAt(cart.getUpdatedAt());

        List<CartItemDto> itemDtos = new ArrayList<>();
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                CartItemDto itemDto = new CartItemDto();
                itemDto.setId(item.getId());
                itemDto.setProductId(item.getProductId());
                itemDto.setQuantity(item.getQuantity());

                try {
                    // Bơm dữ liệu thật vào DTO
                    Product product = productService.getProductById(item.getProductId());
                    itemDto.setProduct(product);
                } catch (Exception e) {
                    System.err.println("Không tìm thấy SP ID: " + item.getProductId());
                }
                itemDtos.add(itemDto);
            }
        }
        dto.setItems(itemDtos);
        return dto;
    }

    // Hàm nội bộ dùng để thao tác với DB
    private Cart getCartEntity(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    return cartRepository.save(newCart);
                });
    }

    // Lấy giỏ hàng (Trả về DTO)
    public CartDto getCartByUserId(Long userId) {
        Cart cart = getCartEntity(userId);
        return mapToDto(cart);
    }

    @Transactional
    public CartDto addToCart(Long userId, CartRequest request) {
        Product product = productService.getProductById(request.getProductId());
        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Vượt quá tồn kho!");
        }

        Cart cart = getCartEntity(userId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);

        return mapToDto(savedCart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartEntity(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional
    public CartDto updateItemQuantity(Long itemId, int quantity) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm này trong giỏ!"));

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        return mapToDto(cartItem.getCart());
    }

    @Transactional
    public void removeItem(Long itemId) {
        cartItemRepository.deleteById(itemId);
    }
}