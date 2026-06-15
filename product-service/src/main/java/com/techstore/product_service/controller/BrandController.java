package com.techstore.product_service.controller;

import com.techstore.product_service.dto.ApiResponse;
import com.techstore.product_service.dto.BrandResponse;
import com.techstore.product_service.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/brands") // Đường dẫn này sẽ map thành /products/brands qua API Gateway của bạn
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    /**
     * Public API: Lấy danh sách thương hiệu đang hoạt động cho cả User và Admin tuyển chọn
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getActiveBrands() {
        List<BrandResponse> data = brandService.getActiveBrands();

        ApiResponse<List<BrandResponse>> res = ApiResponse.<List<BrandResponse>>builder()
                .status("SUCCESS")
                .message("Lấy danh sách thương hiệu thành công")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(res);
    }
}