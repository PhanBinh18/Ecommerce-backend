package com.techstore.product_service.service;

import com.techstore.product_service.dto.ProductDetailResponse;
import com.techstore.product_service.dto.ProductPageResponse;
import com.techstore.product_service.dto.ProductRequest;
import com.techstore.product_service.dto.ProductResponse;
import com.techstore.product_service.entity.Brand;
import com.techstore.product_service.entity.Category;
import com.techstore.product_service.entity.Product;
import com.techstore.product_service.entity.ProductImage;
import com.techstore.product_service.entity.ProductStatus;
import com.techstore.product_service.repository.BrandRepository;
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
public class ProductServiceImpl implements ProductService { // <-- Đã thêm implements

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final BrandRepository brandRepository; // <-- Bổ sung BrandRepository
    private final RedissonClient redissonClient;

    // ======================
    // Public APIs (cached)
    // ======================

    @Override
    @Cacheable(value = "products")
    public ProductPageResponse<ProductResponse> getProducts(int page, int size, String sortType, String keyword, Long categoryId, Long brandId) {
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

        Page<Product> productPage = productRepository.searchAndFilterProducts(keyword, categoryId, brandId, ProductStatus.ACTIVE, pageable);

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

    @Override
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

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDetailResponse createProduct(ProductRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));
        }

        // TÌM BRAND
        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found: " + request.getBrandId()));
        }

        Product p = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .category(category)
                .brand(brand) // <-- Gắn Brand vào Entity
                .stockQuantity(Optional.ofNullable(request.getStockQuantity()).orElse(0))
                .reservedQuantity(0)
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productRepository.save(p);
        return toProductDetailResponse(saved);
    }

    @Override
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

        // CẬP NHẬT BRAND
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found: " + request.getBrandId()));
            product.setBrand(brand);
        }

        Product saved = productRepository.save(product);
        return toProductDetailResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        product.setStatus(ProductStatus.DELETED);
        productRepository.save(product);
    }

    // ======================
    // Stock Reservation & Saga
    // (Các hàm này giữ nguyên 100% logic của bạn)
    // ======================

    @Override
    public boolean reserveStock(Long productId, int quantity) throws InterruptedException {
        String lockKey = "product::lock::" + productId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean lockAcquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!lockAcquired) throw new RuntimeException("Failed to acquire lock for product " + productId);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            int availableStock = product.getStockQuantity() - product.getReservedQuantity();
            if (availableStock < quantity) return false;

            product.setReservedQuantity(product.getReservedQuantity() + quantity);
            productRepository.save(product);
            return true;
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    @Override
    @Transactional
    public void releaseReservation(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        int newReserved = product.getReservedQuantity() - quantity;
        product.setReservedQuantity(Math.max(newReserved, 0));
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void confirmOrder(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.setStockQuantity(product.getStockQuantity() - quantity);
        product.setReservedQuantity(product.getReservedQuantity() - quantity);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void confirmOrderItems(List<com.techstore.product_service.event.OrderItemEvent> items) {
        for (com.techstore.product_service.event.OrderItemEvent item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));
            product.setStockQuantity(Math.max(product.getStockQuantity() - item.getQuantity(), 0));
            product.setReservedQuantity(Math.max(product.getReservedQuantity() - item.getQuantity(), 0));
            productRepository.save(product);
            System.out.println("✅ Confirmed order item: Product #" + item.getProductId() + ", Qty: " + item.getQuantity());
        }
    }

    @Override
    @Transactional
    public void cancelOrderItems(List<com.techstore.product_service.event.OrderItemEvent> items, Boolean isTimeout) {
        for (com.techstore.product_service.event.OrderItemEvent item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductId()));

            if (isTimeout != null && isTimeout) {
                product.setReservedQuantity(Math.max(product.getReservedQuantity() - item.getQuantity(), 0));
                System.out.println("⏱️ Order timeout: Release reserved stock for Product #" + item.getProductId() + ", Qty: " + item.getQuantity());
            } else {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                product.setReservedQuantity(Math.max(product.getReservedQuantity() - item.getQuantity(), 0));
                System.out.println("🔙 Order cancelled (after confirm): Restore stock for Product #" + item.getProductId() + ", Qty: " + item.getQuantity());
            }
            productRepository.save(product);
        }
    }

    // ======================
    // Helpers: mapping entity -> DTO
    // ======================

    private ProductResponse toProductResponse(Product p) {
        String thumbnail = null;

        // Tận dụng danh sách ảnh đã lấy lên cùng Product, không gọi thêm Repository để tránh N+1 Query
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            thumbnail = p.getImages().stream()
                    // Ưu tiên 1: Tìm ảnh được đánh dấu là thumbnail
                    .filter(img -> Boolean.TRUE.equals(img.getIsThumbnail()))
                    .map(ProductImage::getUrl)
                    .findFirst()
                    // Ưu tiên 2 (Kế hoạch dự phòng): Nếu không có, lấy luôn URL của bức ảnh đầu tiên trong mảng
                    .orElse(p.getImages().stream().findFirst().map(ProductImage::getUrl).orElse(null));
        }

        String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;
        String brandName = p.getBrand() != null ? p.getBrand().getName() : null;

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .categoryName(categoryName)
                .brandName(brandName)
                .thumbnail(thumbnail) // <--- Chắc chắn sẽ có link nếu sản phẩm có ảnh
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

        ProductDetailResponse.BrandInfo brandInfo = null;
        if (p.getBrand() != null) {
            brandInfo = ProductDetailResponse.BrandInfo.builder()
                    .id(p.getBrand().getId())
                    .name(p.getBrand().getName())
                    .build();
        }

        return ProductDetailResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .description(p.getDescription())
                .category(catInfo)
                .brand(brandInfo) // Gắn vào DTO (BẠN CẦN BỔ SUNG Class con BrandInfo VÀO ProductDetailResponse.java)
                .images(images)
                .stockQuantity(p.getStockQuantity())
                .createdAt(p.getCreatedAt())
                .build();
    }
}