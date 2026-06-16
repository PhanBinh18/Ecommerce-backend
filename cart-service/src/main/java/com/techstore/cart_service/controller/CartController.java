package com.techstore.cart_service.controller;

import com.techstore.cart_service.dto.*;
import com.techstore.cart_service.entity.Cart;
import com.techstore.cart_service.entity.CartItem;
import com.techstore.cart_service.repository.CartRepository;
import com.techstore.cart_service.service.CartService;
import com.techstore.cart_service.service.GuestCartService;
import com.techstore.cart_service.client.ProductClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private GuestCartService guestCartService;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private CartRepository cartRepository;

    // ---------- Helpers ----------
    /**
     * Return current userId if authenticated (principal is Long), otherwise null.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    private CartResponse toCartResponseFromCartDto(CartDto dto) {
        CartResponse resp = new CartResponse();
        List<CartItemDto> items = dto.getItems() != null ? dto.getItems() : Collections.emptyList();
        resp.setItems(items);
        BigDecimal total = items.stream()
                .map(i -> i.getSubTotal() == null ? BigDecimal.ZERO : i.getSubTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resp.setTotalAmount(total);
        return resp;
    }

    private CartResponse toCartResponseFromGuestItems(List<GuestCartItemDto> guestItems) {
        CartResponse resp = new CartResponse();
        List<CartItemDto> items = guestItems.stream().map(g -> {
            CartItemDto ci = new CartItemDto();
            ci.setProductId(g.getProductId());
            ci.setProductName(g.getProductName());
            ci.setThumbnailUrl(g.getThumbnailUrl());
            ci.setPrice(g.getPrice());
            ci.setQuantity(g.getQuantity());
            ci.setSubTotal(g.getSubTotal());
            return ci;
        }).collect(Collectors.toList());
        resp.setItems(items);
        BigDecimal total = items.stream()
                .map(i -> i.getSubTotal() == null ? BigDecimal.ZERO : i.getSubTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resp.setTotalAmount(total);
        return resp;
    }

    private ResponseEntity<ApiResponse<CartResponse>> unauthorizedResponse(String message) {
        ApiResponse<CartResponse> body = new ApiResponse<>("ERROR", message, null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // ---------- Endpoints ----------

    /**
     * GET /api/v1/carts
     * - user => DB cart
     * - guest => Redis cart
     */
    @GetMapping("/")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader(value = "X-Guest-UUID", required = false) String guestUuid
    ) {
        Long userId = getCurrentUserId();

        if (userId != null) {
            CartDto dto = cartService.getCartByUserId(userId);
            CartResponse resp = toCartResponseFromCartDto(dto);
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "OK", resp));
        }

        if (guestUuid != null && !guestUuid.isBlank()) {
            List<GuestCartItemDto> guestItems = guestCartService.getCart(guestUuid);
            CartResponse resp = toCartResponseFromGuestItems(guestItems);
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "OK", resp));
        }

        return unauthorizedResponse("Unauthorized: missing authentication or guest identifier");
    }

    /**
     * POST /api/v1/carts/items
     * Add item (user or guest)
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @RequestHeader(value = "X-Guest-UUID", required = false) String guestUuid,
            @RequestBody CartRequest request
    ) {
        Long userId = getCurrentUserId();
        if (userId != null) {
            // add to DB cart
            try {
                CartDto dto = cartService.addToCart(userId, request.getProductId(), request.getQuantity());
                CartResponse resp = toCartResponseFromCartDto(dto);
                return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Added to cart", resp));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse<>("ERROR", e.getMessage(), null));
            }
        }

        if (guestUuid != null && !guestUuid.isBlank()) {
            // For guest, we need product detail to snapshot
            ApiResponse<ProductDetailResponse> productResp;
            try {
                productResp = productClient.getProductById(request.getProductId());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(new ApiResponse<>("ERROR", "Product Service unreachable or product not found", null));
            }
            if (productResp == null || productResp.getData() == null) {
                return ResponseEntity.badRequest().body(new ApiResponse<>("ERROR", "Product not found", null));
            }
            ProductDetailResponse product = productResp.getData();
            // stock check
            if (product.getStockQuantity() == null || product.getStockQuantity() < request.getQuantity()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>("ERROR", "Insufficient stock. Available: " + product.getStockQuantity(), null));
            }

            // add to guest redis
            guestCartService.addItem(guestUuid, request.getProductId(), request.getQuantity(), product);

            // return updated guest cart
            List<GuestCartItemDto> guestItems = guestCartService.getCart(guestUuid);
            CartResponse resp = toCartResponseFromGuestItems(guestItems);
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Added to guest cart", resp));
        }

        return unauthorizedResponse("Unauthorized: missing authentication or guest identifier");
    }

    /**
     * PUT /api/v1/carts/items/{productId}
     * Update quantity by productId for user or guest.
     */
    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @RequestHeader(value = "X-Guest-UUID", required = false) String guestUuid,
            @PathVariable Long productId,
            @RequestBody Map<String, Integer> payload
    ) {
        Integer quantity = payload.get("quantity");
        if (quantity == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("ERROR", "Missing quantity in body", null));
        }

        Long userId = getCurrentUserId();
        if (userId != null) {
            // find cart item id by productId
            Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
            if (cartOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>("ERROR", "Cart not found", null));
            }
            Cart cart = cartOpt.get();
            Optional<CartItem> ciOpt = cart.getItems().stream()
                    .filter(ci -> Objects.equals(ci.getProductId(), productId))
                    .findFirst();
            if (ciOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>("ERROR", "Item not found in cart", null));
            }
            CartItem ci = ciOpt.get();
            try {
                CartDto dto = cartService.updateItemQuantity(ci.getId(), quantity);
                CartResponse resp = toCartResponseFromCartDto(dto);
                return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Quantity updated", resp));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse<>("ERROR", e.getMessage(), null));
            }
        }

        if (guestUuid != null && !guestUuid.isBlank()) {
            try {
                guestCartService.updateItemQuantity(guestUuid, productId, quantity);
                List<GuestCartItemDto> guestItems = guestCartService.getCart(guestUuid);
                CartResponse resp = toCartResponseFromGuestItems(guestItems);
                return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Guest quantity updated", resp));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new ApiResponse<>("ERROR", e.getMessage(), null));
            }
        }

        return unauthorizedResponse("Unauthorized: missing authentication or guest identifier");
    }

    /**
     * DELETE /api/v1/carts/items/{productId}
     * Remove item by productId for user or guest.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @RequestHeader(value = "X-Guest-UUID", required = false) String guestUuid,
            @PathVariable Long productId
    ) {
        Long userId = getCurrentUserId();
        if (userId != null) {
            // ĐÃ SỬA: Gọi trực tiếp hàm xóa mới tạo trong Service
            cartService.removeProductFromUserCart(userId, productId);

            // Lấy lại giỏ hàng mới nhất trả về cho Frontend
            CartDto dto = cartService.getCartByUserId(userId);
            CartResponse resp = toCartResponseFromCartDto(dto);
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Item removed", resp));
        }

        if (guestUuid != null && !guestUuid.isBlank()) {
            guestCartService.removeItem(guestUuid, productId);
            List<GuestCartItemDto> guestItems = guestCartService.getCart(guestUuid);
            CartResponse resp = toCartResponseFromGuestItems(guestItems);
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Item removed from guest cart", resp));
        }

        return unauthorizedResponse("Unauthorized: missing authentication or guest identifier");
    }

    /**
     * POST /api/v1/carts/merge
     * Merge guest cart into logged-in user's DB cart. guestUuid from header or request param.
     * Requires user to be authenticated.
     */
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<String>> mergeGuestCart(
            @RequestHeader(value = "X-Guest-UUID", required = false) String guestUuidHeader,
            @RequestParam(value = "guestUuid", required = false) String guestUuidParam
    ) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>("ERROR", "Authentication required for merge", null));
        }
        String guestUuid = (guestUuidHeader != null && !guestUuidHeader.isBlank()) ? guestUuidHeader : guestUuidParam;
        if (guestUuid == null || guestUuid.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("ERROR", "guestUuid is required", null));
        }

        try {
            cartService.mergeGuestCartToUser(guestUuid, userId);
            return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Merged guest cart into user cart", "OK"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>("ERROR", e.getMessage(), null));
        }
    }
}