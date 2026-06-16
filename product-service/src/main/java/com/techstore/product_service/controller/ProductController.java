package com.techstore.product_service.controller;

import com.techstore.product_service.dto.*;
import com.techstore.product_service.service.ProductImageService;
import com.techstore.product_service.service.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductServiceImpl productServiceImpl;
    private final ProductImageService productImageService;

    // -----------------------
    // Public endpoints
    // -----------------------

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<ProductPageResponse<ProductResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sortType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, value = "category") Long categoryId, // ĐÃ SỬA: Đổi sang Long
            @RequestParam(required = false, value = "brand") Long brandId) {     // ĐÃ SỬA: Đổi sang Long

        // ĐÃ SỬA: Truyền categoryId và brandId vào Service
        ProductPageResponse<ProductResponse> data = productServiceImpl.getProducts(page, size, sortType, keyword, categoryId, brandId);

        ApiResponse<ProductPageResponse<ProductResponse>> res = ApiResponse.<ProductPageResponse<ProductResponse>>builder()
                .status("SUCCESS")
                .message("Lấy danh sách sản phẩm thành công")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable Long id) {
        ProductDetailResponse data = productServiceImpl.getProductById(id);
        ApiResponse<ProductDetailResponse> res = ApiResponse.<ProductDetailResponse>builder()
                .status("SUCCESS")
                .message("Lấy chi tiết sản phẩm thành công")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(res);
    }

    // -----------------------
    // Admin endpoints
    // -----------------------
    // Note: add security annotations later (e.g. @PreAuthorize)

    @PostMapping("/admin/products")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(@RequestBody ProductRequest request) {
        ProductDetailResponse data = productServiceImpl.createProduct(request);
        ApiResponse<ProductDetailResponse> res = ApiResponse.<ProductDetailResponse>builder()
                .status("SUCCESS")
                .message("Tạo sản phẩm thành công")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(res);
    }

    @PutMapping("/admin/products/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        ProductDetailResponse data = productServiceImpl.updateProduct(id, request);
        ApiResponse<ProductDetailResponse> res = ApiResponse.<ProductDetailResponse>builder()
                .status("SUCCESS")
                .message("Cập nhật sản phẩm thành công")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productServiceImpl.deleteProduct(id);
        ApiResponse<Void> res = ApiResponse.<Void>builder()
                .status("SUCCESS")
                .message("Đã xóa mềm (soft delete) sản phẩm")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(res);
    }

    /**
     * Upload images for a product (admin)
     * Example: multipart/form-data, key "files" for multiple.
     */
    @PostMapping("/admin/products/{id}/images")
    public ResponseEntity<ApiResponse<List<String>>> uploadProductImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            List<?> saved = productImageService.uploadImages(id, files);
            // return list of URLs
            List<String> urls = saved.stream().map(obj -> {
                try {
                    // ProductImage has getUrl()
                    return (String) obj.getClass().getMethod("getUrl").invoke(obj);
                } catch (Exception e) {
                    return null;
                }
            }).filter(u -> u != null).toList();

            ApiResponse<List<String>> res = ApiResponse.<List<String>>builder()
                    .status("SUCCESS")
                    .message("Upload ảnh thành công")
                    .data(urls)
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            ApiResponse<List<String>> res = ApiResponse.<List<String>>builder()
                    .status("ERROR")
                    .message("Lỗi khi upload ảnh: " + e.getMessage())
                    .data(null)
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.status(500).body(res);
        }
    }
}