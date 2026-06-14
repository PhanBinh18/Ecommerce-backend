package com.techstore.cart_service.service;

import com.techstore.cart_service.client.ProductClient;
import com.techstore.cart_service.dto.*;
import com.techstore.cart_service.entity.Cart;
import com.techstore.cart_service.entity.CartItem;
import com.techstore.cart_service.repository.CartItemRepository;
import com.techstore.cart_service.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private GuestCartService guestCartService;

    // =================================================================
    // HÀM BỔ TRỢ: Chuyển đổi từ Cart (Entity) sang CartDto (Gửi về React)
    // Sử dụng snapshot fields (không gọi Product Service)
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
                itemDto.setProductId(item.getProductId());
                itemDto.setProductName(item.getProductName());
                itemDto.setThumbnailUrl(item.getThumbnailUrl());
                itemDto.setPrice(item.getPrice());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setSubTotal(item.getSubTotal());
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

    // Giữ nguyên để không phá vỡ controller hiện tại (nếu controller vẫn gọi qua CartRequest)
    @Transactional
    public CartDto addToCart(Long userId, com.techstore.cart_service.dto.CartRequest request) {
        return addToCart(userId, request.getProductId(), request.getQuantity());
    }

    /**
     * Thêm sản phẩm vào giỏ MySQL của user, lưu snapshot từ Product Service.
     */
    @Transactional
    public CartDto addToCart(Long userId, Long productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0");

        // gọi Product Service qua Feign để lấy chi tiết (ApiResponse<ProductDetailResponse>)
        ApiResponse<ProductDetailResponse> productResp;
        try {
            productResp = productClient.getProductById(productId);
        } catch (Exception e) {
            throw new RuntimeException("Không thể liên lạc Product Service hoặc sản phẩm không tồn tại", e);
        }

        if (productResp == null || productResp.getData() == null) {
            throw new RuntimeException("Sản phẩm không tồn tại hoặc Product Service trả về rỗng");
        }

        ProductDetailResponse product = productResp.getData();

        if (product.getStockQuantity() == null || product.getStockQuantity() < quantity) {
            throw new RuntimeException("Vượt quá tồn kho! Chỉ còn " + product.getStockQuantity() + " sản phẩm.");
        }

        Cart cart = getCartEntity(userId);

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            int newQty = item.getQuantity() + quantity;
            item.setQuantity(newQty);
            // update snapshot price/name/thumbnail to latest product snapshot
            item.setProductName(product.getName());
            item.setThumbnailUrl(product.getThumbnail());
            item.setPrice(product.getPrice());
            item.setSubTotal(product.getPrice().multiply(BigDecimal.valueOf(newQty)));
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            newItem.setProductName(product.getName());
            newItem.setThumbnailUrl(product.getThumbnail());
            newItem.setPrice(product.getPrice());
            newItem.setSubTotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);

        return mapToDto(savedCart);
    }

    /**
     * Merge guest cart from Redis into user's MySQL cart, then clear guest cart.
     */
    @Transactional
    public void mergeGuestCartToUser(String guestUuid, Long userId) {
        if (guestUuid == null || guestUuid.isEmpty()) return;

        List<GuestCartItemDto> guestItems = guestCartService.getCart(guestUuid);
        if (guestItems == null || guestItems.isEmpty()) {
            // nothing to merge, but ensure guest key removed
            guestCartService.clearCart(guestUuid);
            return;
        }

        Cart cart = getCartEntity(userId);

        for (GuestCartItemDto gItem : guestItems) {
            Long productId = gItem.getProductId();
            if (productId == null) continue;

            Optional<CartItem> existingOpt = cart.getItems().stream()
                    .filter(ci -> ci.getProductId().equals(productId))
                    .findFirst();

            if (existingOpt.isPresent()) {
                CartItem existing = existingOpt.get();
                int newQty = existing.getQuantity() + (gItem.getQuantity() == null ? 0 : gItem.getQuantity());
                existing.setQuantity(newQty);
                // update snapshot fields from guest item (guest snapshot should be used)
                existing.setProductName(gItem.getProductName());
                existing.setThumbnailUrl(gItem.getThumbnailUrl());
                if (gItem.getPrice() != null) {
                    existing.setPrice(gItem.getPrice());
                }
                if (existing.getPrice() != null) {
                    existing.setSubTotal(existing.getPrice().multiply(BigDecimal.valueOf(newQty)));
                }
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProductId(productId);
                newItem.setQuantity(gItem.getQuantity() == null ? 0 : gItem.getQuantity());
                newItem.setProductName(gItem.getProductName());
                newItem.setThumbnailUrl(gItem.getThumbnailUrl());
                newItem.setPrice(gItem.getPrice());
                if (gItem.getPrice() != null) {
                    newItem.setSubTotal(gItem.getPrice().multiply(BigDecimal.valueOf(newItem.getQuantity())));
                } else {
                    newItem.setSubTotal(BigDecimal.ZERO);
                }
                cart.getItems().add(newItem);
            }
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        // clear the guest cart from Redis to free memory
        guestCartService.clearCart(guestUuid);
    }

    /**
     * Clear user cart (DB).
     */
    @Transactional
    public void clearUserCart(Long userId) {
        Cart cart = getCartEntity(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // Existing DB-based operations (kept for compatibility)
    @Transactional
    public CartDto updateItemQuantity(Long itemId, int quantity) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm này trong giỏ!"));

        cartItem.setQuantity(quantity);
        // recompute subTotal if price present
        if (cartItem.getPrice() != null) {
            cartItem.setSubTotal(cartItem.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        cartItemRepository.save(cartItem);

        return mapToDto(cartItem.getCart());
    }

    @Transactional
    public void removeItem(Long itemId) {
        cartItemRepository.deleteById(itemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        clearUserCart(userId);
    }
}