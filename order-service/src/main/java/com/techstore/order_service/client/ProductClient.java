package com.techstore.order_service.client;

import com.techstore.order_service.config.FeignConfig;
import com.techstore.order_service.dto.ApiResponse;
import com.techstore.order_service.dto.ProductDetailResponse;
import com.techstore.order_service.dto.ProductResponseDTO;
import com.techstore.order_service.dto.ReservationRequest;
import com.techstore.order_service.dto.ReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ApiResponse<ProductDetailResponse> getProductById(@PathVariable("id") Long id);

    @PutMapping("/api/v1/products/{id}/reduce-stock")
    ApiResponse<ProductResponseDTO> reduceStock(@PathVariable("id") Long productId, @RequestParam("quantity") int quantity);

    @PutMapping("/api/v1/products/{id}/increase-stock")
    ApiResponse<Void> increaseStock(@PathVariable("id") Long productId, @RequestParam("quantity") int quantity);

    @PostMapping("/api/v1/internal/products/reserve")
    ApiResponse<List<ReservationResponse>> reserveStock(@RequestBody List<ReservationRequest> requests);

    @PutMapping("/api/v1/internal/products/release-reservation")
    ApiResponse<Object> releaseReservation(@RequestBody List<ReservationRequest> requests);
}