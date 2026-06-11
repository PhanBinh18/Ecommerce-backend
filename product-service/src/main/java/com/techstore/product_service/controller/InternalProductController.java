package com.techstore.product_service.controller;

import com.techstore.product_service.dto.ApiResponse;
import com.techstore.product_service.dto.ReservationRequest;
import com.techstore.product_service.dto.ReservationResponse;
import com.techstore.product_service.entity.Product;
import com.techstore.product_service.repository.ProductRepository;
import com.techstore.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/products")
public class InternalProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;

    /**
     * Check stock availability for a product.
     * GET /api/v1/internal/products/{id}/check-stock?quantity=X
     */
    @GetMapping("/{id}/check-stock")
    public ResponseEntity<ApiResponse<CheckStockResponse>> checkStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + id));

            int availableStock = product.getStockQuantity() - product.getReservedQuantity();
            boolean available = availableStock >= quantity;

            CheckStockResponse data = CheckStockResponse.builder()
                    .available(available)
                    .currentStock(availableStock)
                    .build();

            ApiResponse<CheckStockResponse> res = ApiResponse.<CheckStockResponse>builder()
                    .status("SUCCESS")
                    .message("Stock check completed")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            ApiResponse<CheckStockResponse> res = ApiResponse.<CheckStockResponse>builder()
                    .status("ERROR")
                    .message(e.getMessage())
                    .data(null)
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Reserve stock for one or more products.
     * POST /api/v1/internal/products/reserve
     * Request body: List<ReservationRequest>
     */
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> reserveStock(
            @RequestBody List<ReservationRequest> requests) {
        try {
            List<ReservationResponse> responses = requests.stream()
                    .map(req -> {
                        try {
                            boolean reserved = productService.reserveStock(req.getProductId(), req.getQuantity());
                            return ReservationResponse.builder()
                                    .productId(req.getProductId())
                                    .reserved(reserved)
                                    .message(reserved ? "Stock reserved successfully" : "Not enough stock")
                                    .build();
                        } catch (Exception e) {
                            return ReservationResponse.builder()
                                    .productId(req.getProductId())
                                    .reserved(false)
                                    .message("Error: " + e.getMessage())
                                    .build();
                        }
                    })
                    .collect(Collectors.toList());

            ApiResponse<List<ReservationResponse>> res = ApiResponse.<List<ReservationResponse>>builder()
                    .status("SUCCESS")
                    .message("Stock reservation processing completed")
                    .data(responses)
                    .timestamp(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            ApiResponse<List<ReservationResponse>> res = ApiResponse.<List<ReservationResponse>>builder()
                    .status("ERROR")
                    .message("Error during reservation: " + e.getMessage())
                    .data(null)
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(500).body(res);
        }
    }

    /**
     * Release reserved stock when order is cancelled.
     * PUT /api/v1/internal/products/release-reservation
     * Request body: List<ReservationRequest>
     */
    @PutMapping("/release-reservation")
    public ResponseEntity<ApiResponse<ReleaseResponse>> releaseReservation(
            @RequestBody List<ReservationRequest> requests) {
        try {
            int totalReleased = 0;
            int totalFailed = 0;

            for (ReservationRequest req : requests) {
                try {
                    productService.releaseReservation(req.getProductId(), req.getQuantity());
                    totalReleased++;
                } catch (Exception e) {
                    totalFailed++;
                }
            }

            ReleaseResponse data = ReleaseResponse.builder()
                    .totalProcessed(requests.size())
                    .totalReleased(totalReleased)
                    .totalFailed(totalFailed)
                    .build();

            ApiResponse<ReleaseResponse> res = ApiResponse.<ReleaseResponse>builder()
                    .status("SUCCESS")
                    .message("Release reservation completed")
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            ApiResponse<ReleaseResponse> res = ApiResponse.<ReleaseResponse>builder()
                    .status("ERROR")
                    .message("Error during release: " + e.getMessage())
                    .data(null)
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(500).body(res);
        }
    }

    // ==================
    // Inner DTOs for Response
    // ==================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CheckStockResponse {
        private Boolean available;
        private Integer currentStock;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReleaseResponse {
        private Integer totalProcessed;
        private Integer totalReleased;
        private Integer totalFailed;
    }
}