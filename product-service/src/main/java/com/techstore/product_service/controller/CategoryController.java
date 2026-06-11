package com.techstore.product_service.controller;

import com.techstore.product_service.dto.ApiResponse;
import com.techstore.product_service.dto.CategoryRequest;
import com.techstore.product_service.dto.CategoryResponse;
import com.techstore.product_service.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CategoryController {

    private final CategoryService categoryService;

    // Public: only DISPLAY
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getPublicCategories() {
        List<CategoryResponse> data = categoryService.getDisplayCategories();
        ApiResponse<List<CategoryResponse>> res = ApiResponse.<List<CategoryResponse>>builder()
                .status("SUCCESS")
                .message("Lấy danh sách danh mục thành công")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(res);
    }

    // Admin: list all categories
    @GetMapping("/admin/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategoriesForAdmin() {
        List<CategoryResponse> data = categoryService.getAllCategoriesForAdmin();
        ApiResponse<List<CategoryResponse>> res = ApiResponse.<List<CategoryResponse>>builder()
                .status("SUCCESS")
                .message("Lấy danh sách danh mục (admin) thành công")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(res);
    }

    // Admin: create
    @PostMapping("/admin/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(request);
        ApiResponse<CategoryResponse> res = ApiResponse.<CategoryResponse>builder()
                .status("SUCCESS")
                .message("Tạo danh mục thành công")
                .data(created)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(res);
    }

    // Admin: update name
    @PutMapping("/admin/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {
        CategoryResponse updated = categoryService.updateCategory(id, request);
        ApiResponse<CategoryResponse> res = ApiResponse.<CategoryResponse>builder()
                .status("SUCCESS")
                .message("Cập nhật danh mục thành công")
                .data(updated)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(res);
    }

    // Admin: hide category (soft delete)
    @DeleteMapping("/admin/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> hideCategory(@PathVariable Long id) {
        CategoryResponse hidden = categoryService.hideCategory(id);
        ApiResponse<CategoryResponse> res = ApiResponse.<CategoryResponse>builder()
                .status("SUCCESS")
                .message("Đã ẩn danh mục thành công")
                .data(hidden)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(res);
    }
}