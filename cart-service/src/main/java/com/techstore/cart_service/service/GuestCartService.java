package com.techstore.cart_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techstore.cart_service.dto.GuestCartItemDto;
import com.techstore.cart_service.dto.ProductDetailResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GuestCartService {

    private static final String KEY_PREFIX = "cart:guest:"; // full key: cart:guest:{guestUuid}
    private static final Duration TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public GuestCartService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // don't fail on unknown props to tolerate different stored shapes
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private String makeKey(String guestUuid) {
        return KEY_PREFIX + guestUuid;
    }

    /**
     * Get all items in guest cart (as list).
     */
    public List<GuestCartItemDto> getCart(String guestUuid) {
        String key = makeKey(guestUuid);
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        Map<String, Object> entries = ops.entries(key);
        List<GuestCartItemDto> items = new ArrayList<>();
        if (entries == null || entries.isEmpty()) return items;

        for (Object value : entries.values()) {
            if (value == null) continue;
            GuestCartItemDto dto;
            if (value instanceof GuestCartItemDto) {
                dto = (GuestCartItemDto) value;
            } else {
                // convert via ObjectMapper (in case value is LinkedHashMap etc.)
                dto = objectMapper.convertValue(value, GuestCartItemDto.class);
            }
            items.add(dto);
        }
        return items;
    }

    /**
     * Add item to guest cart (or increase quantity if exists). Snapshot product info.
     */
    public void addItem(String guestUuid, Long productId, int quantity, ProductDetailResponse product) {
        if (product == null) throw new IllegalArgumentException("Product detail is required");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0");

        String key = makeKey(guestUuid);
        String field = String.valueOf(productId);
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();

        Object existing = ops.get(key, field);
        GuestCartItemDto item;
        if (existing != null) {
            if (existing instanceof GuestCartItemDto) {
                item = (GuestCartItemDto) existing;
            } else {
                item = objectMapper.convertValue(existing, GuestCartItemDto.class);
            }
            // accumulate
            int newQty = item.getQuantity() + quantity;
            item.setQuantity(newQty);
            if (item.getPrice() != null) {
                item.setSubTotal(item.getPrice().multiply(java.math.BigDecimal.valueOf(newQty)));
            }
        } else {
            item = new GuestCartItemDto();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setThumbnailUrl(product.getThumbnail());
            item.setPrice(product.getPrice());
            item.setQuantity(quantity);
            if (product.getPrice() != null) {
                item.setSubTotal(product.getPrice().multiply(java.math.BigDecimal.valueOf(quantity)));
            } else {
                item.setSubTotal(java.math.BigDecimal.ZERO);
            }
        }

        ops.put(key, field, item);
        // reset TTL
        redisTemplate.expire(key, TTL);
    }

    /**
     * Update quantity for a product in guest cart.
     */
    public void updateItemQuantity(String guestUuid, Long productId, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity must be >= 0");
        String key = makeKey(guestUuid);
        String field = String.valueOf(productId);
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();

        Object existing = ops.get(key, field);
        if (existing == null) {
            throw new RuntimeException("Item not found in guest cart");
        }

        GuestCartItemDto item;
        if (existing instanceof GuestCartItemDto) {
            item = (GuestCartItemDto) existing;
        } else {
            item = objectMapper.convertValue(existing, GuestCartItemDto.class);
        }

        if (quantity == 0) {
            // remove
            ops.delete(key, field);
        } else {
            item.setQuantity(quantity);
            if (item.getPrice() != null) {
                item.setSubTotal(item.getPrice().multiply(java.math.BigDecimal.valueOf(quantity)));
            }
            ops.put(key, field, item);
        }

        // if we still have the key, refresh TTL
        redisTemplate.expire(key, TTL);
    }

    /**
     * Remove one product from guest cart.
     */
    public void removeItem(String guestUuid, Long productId) {
        String key = makeKey(guestUuid);
        String field = String.valueOf(productId);
        HashOperations<String, String, Object> ops = redisTemplate.opsForHash();
        ops.delete(key, field);
        // refresh TTL if still exists
        redisTemplate.expire(key, TTL);
    }

    /**
     * Clear entire guest cart (delete key)
     */
    public void clearCart(String guestUuid) {
        String key = makeKey(guestUuid);
        redisTemplate.delete(key);
    }
}