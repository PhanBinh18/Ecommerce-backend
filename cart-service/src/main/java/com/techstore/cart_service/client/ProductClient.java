package com.techstore.cart_service.client;

import com.techstore.cart_service.dto.ApiResponse;
import com.techstore.cart_service.dto.ProductDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client to Product Service.
 * Must match Product Service contract: GET /api/v1/products/{id} -> ApiResponse<ProductDetailResponse>
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ApiResponse<ProductDetailResponse> getProductById(@PathVariable("id") Long id);
}