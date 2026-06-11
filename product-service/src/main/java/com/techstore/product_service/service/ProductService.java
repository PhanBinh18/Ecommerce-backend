package com.techstore.product_service.service;

import com.techstore.product_service.dto.ProductDetailResponse;
import com.techstore.product_service.dto.ProductPageResponse;
import com.techstore.product_service.dto.ProductRequest;
import com.techstore.product_service.dto.ProductResponse;
import com.techstore.product_service.entity.Category;
import com.techstore.product_service.entity.Product;
import com.techstore.product_service.entity.ProductImage;
import com.techstore.product_service.entity.ProductStatus;
import com.techstore.product_service.repository.CategoryRepository;
import com.techstore.product_service.repository.ProductImageRepository;
import com.techstore.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final RedissonClient redissonClient;

    // ======================
    // Public APIs (cached)
    // ======================

    @Cacheable(value = "products")
    public ProductPageResponse<ProductResponse> getProducts(int page, int size, String sortType, String keyword, String category, String brand) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if (sortType != null && !sortType.isEmpty()) {
            switch (sortType) {
                case "name_asc":
                    sort = Sort.by(Sort.Direction.ASC, "name");
                    break;
                case "name_desc":
                    sort = Sort.by(Sort.Direction.DESC, "name");
                    break;
                case "price_asc":
                    sort = Sort.by(Sort.Direction.ASC, "price");
                    break;
                case "price_desc":
                    sort = Sort.by(Sort.Direction.DESC, "price");
                    break;
                default:
                    sort = Sort.by(Sort.Direction.DESC, "id");
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.searchAndFilterProducts(keyword, category, ProductStatus.ACTIVE, pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        return ProductPageResponse.<ProductResponse>builder()
                .content(content)
                .pageNumber(productPage.getNumber())
                .pageSize(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .build();
    }

    @Cacheable(value = "products", key = "'product::' + #id")
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new RuntimeException("Product is not available");
        }

        return toProductDetailResponse(product);
    }

    // ======================
    // Admin APIs (mutating)
    // ======================

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDetailResponse createProduct(ProductRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));
        }

        Product p = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .category(category)
                .stockQuantity(Optional.ofNullable(request.getStockQuantity()).orElse(0))
                .reservedQuantity(0)
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productRepository.save(p);
        return toProductDetailResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDetailResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        return toProductDetailResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        product.setStatus(ProductStatus.DELETED);
        productRepository.save(product);
    }

    // ======================
    // Stock Reservation (with Redisson Lock)
    // ======================

    /**
     * Reserve stock for an order. Uses Redisson RLock to prevent race conditions.
     * Returns true if reservation succeeded, false if not enough stock.
     */
    public boolean reserveStock(Long productId, int quantity) throws InterruptedException {
        String lockKey = "product::lock::" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire lock with 10 second wait time and 30 second hold time
            boolean lockAcquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!lockAcquired) {
                throw new RuntimeException("Failed to acquire lock for product " + productId);
            }

            // Lock acquired, now check & update stock
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            // Check if enough stock available
            int availableStock = product.getStockQuantity() - product.getReservedQuantity();
            if (availableStock < quantity) {
                return false; // Not enough stock
            }

            // Reserve the stock
            product.setReservedQuantity(product.getReservedQuantity() + quantity);
            productRepository.save(product);

            return true; // Reservation successful
        } finally {
            // Always unlock
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Release a reservation when order is cancelled (timeout or other reason).
     * Subtract from reservedQuantity only (don't add back to stockQuantity).
     */
    @Transactional
    public void releaseReservation(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        int newReserved = product.getReservedQuantity() - quantity;
        if (newReserved < 0) newReserved = 0;

        product.setReservedQuantity(newReserved);
        productRepository.save(product);
    }

    /**
     * Confirm order: deduct both stockQuantity and reservedQuantity.
     * Called after successful payment.
     */
    @Transactional
    public void confirmOrder(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        // Deduct from stock
        product.setStockQuantity(product.getStockQuantity() - quantity);
        // Deduct from reserved
        product.setReservedQuantity(product.getReservedQuantity() - quantity);

        productRepository.save(product);
    }

    // ======================
    // Saga Pattern: Order Confirmation
    // ======================

    /**     * Called when ORDER_CONFIRMED event received from Order Service.     * Deduct both stockQuantity and reservedQuantity for confirmed items.     * Wrapped in @Transactional for atomicity.     */
    @Transactional
    public void confirmOrderItems(List<com.techstore.product_service.event.OrderItemEvent> items) {
        for (com.techstore.product_service.event.OrderItemEvent item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

            // Deduct from stock (actual inventory)
            int newStock = product.getStockQuantity() - item.getQuantity();
            if (newStock < 0) newStock = 0;
            product.setStockQuantity(newStock);

            // Deduct from reserved (already held for this order)
            int newReserved = product.getReservedQuantity() - item.getQuantity();
            if (newReserved < 0) newReserved = 0;
            product.setReservedQuantity(newReserved);

            productRepository.save(product);
            System.out.println("✅ Confirmed order item: Product #" + item.getProductId()
                    + ", Qty: " + item.getQuantity());
        }
    }

    // ======================
    // Saga Pattern: Order Cancellation
    // ======================

    /**     * Called when ORDER_CANCELLED event received from Order Service.     *
     * Logic:     * - If isTimeout == true: Only release reserved stock (payment timeout, no shipment yet)     * - If isTimeout == false: Add back to stock (order confirmed but later cancelled)     *
     * Wrapped in @Transactional for atomicity.     */
    @Transactional
    public void cancelOrderItems(List<com.techstore.product_service.event.OrderItemEvent> items, Boolean isTimeout) {
        for (com.techstore.product_service.event.OrderItemEvent item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

            if (isTimeout != null && isTimeout) {
                // ===== TIMEOUT CASE =====
                // Payment timeout: reserved stock was never deducted from actual stock
                // Just release the reservation
                int newReserved = product.getReservedQuantity() - item.getQuantity();
                if (newReserved < 0) newReserved = 0;
                product.setReservedQuantity(newReserved);

                System.out.println("⏱️ Order timeout: Release reserved stock for Product #"
                        + item.getProductId() + ", Qty: " + item.getQuantity());
            } else {
                // ===== CANCELLED AFTER CONFIRMED CASE =====
                // Order was confirmed and stock was deducted
                // Now we need to add it back to inventory
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());

                // Also reduce reserved if still tracked (safeguard)
                int newReserved = product.getReservedQuantity() - item.getQuantity();
                if (newReserved < 0) newReserved = 0;
                product.setReservedQuantity(newReserved);

                System.out.println("🔙 Order cancelled (after confirm): Restore stock for Product #"
                        + item.getProductId() + ", Qty: " + item.getQuantity());
            }

            productRepository.save(product);
        }
    }

    // ======================
    // Helpers: mapping entity -> DTO
    // ======================

    private ProductResponse toProductResponse(Product p) {
        String thumbnail = null;
        Optional<ProductImage> thumb = productImageRepository.findByProductIdAndIsThumbnailTrue(p.getId());
        if (thumb.isPresent()) thumbnail = thumb.get().getUrl();

        String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .categoryName(categoryName)
                .thumbnail(thumbnail)
                .build();
    }

    private ProductDetailResponse toProductDetailResponse(Product p) {
        List<ProductDetailResponse.ImageInfo> images = p.getImages().stream()
                .map(img -> ProductDetailResponse.ImageInfo.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .isThumbnail(img.getIsThumbnail())
                        .build())
                .collect(Collectors.toList());

        ProductDetailResponse.CategoryInfo catInfo = null;
        if (p.getCategory() != null) {
            catInfo = ProductDetailResponse.CategoryInfo.builder()
                    .id(p.getCategory().getId())
                    .name(p.getCategory().getName())
                    .build();
        }

        return ProductDetailResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .description(p.getDescription())
                .category(catInfo)
                .images(images)
                .stockQuantity(p.getStockQuantity())
                .createdAt(p.getCreatedAt())
                .build();
    }
}