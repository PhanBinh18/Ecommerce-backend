package com.techstore.product_service.service;

import com.techstore.product_service.dto.ProductDetailResponse;
import com.techstore.product_service.dto.ProductPageResponse;
import com.techstore.product_service.dto.ProductRequest;
import com.techstore.product_service.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    // ======================
    // Public APIs
    // ======================
    ProductPageResponse<ProductResponse> getProducts(int page, int size, String sortType, String keyword, Long categoryId, Long brandId);

    ProductDetailResponse getProductById(Long id);

    // ======================
    // Admin APIs
    // ======================
    ProductDetailResponse createProduct(ProductRequest request);

    ProductDetailResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    // ======================
    // Stock Reservation & Saga
    // ======================
    boolean reserveStock(Long productId, int quantity) throws InterruptedException;

    void releaseReservation(Long productId, int quantity);

    void confirmOrder(Long productId, int quantity);

    void confirmOrderItems(List<com.techstore.product_service.event.OrderItemEvent> items);

    void cancelOrderItems(List<com.techstore.product_service.event.OrderItemEvent> items, Boolean isTimeout);

    // ĐÃ THÊM: Hàm cộng lại kho khi hủy đơn
    void increaseStock(Long productId, int quantity);
}