package com.techstore.product_service.service;

import com.techstore.product_service.dto.CategoryRequest;
import com.techstore.product_service.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

    // Lấy danh sách hiển thị cho khách hàng
    List<CategoryResponse> getDisplayCategories();

    // Lấy toàn bộ danh sách cho Admin
    List<CategoryResponse> getAllCategoriesForAdmin();

    // Lấy chi tiết theo ID
    CategoryResponse getCategoryById(Long id);

    // Tạo mới danh mục
    CategoryResponse createCategory(CategoryRequest request);

    // Cập nhật danh mục
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    // Ẩn danh mục (Soft delete)
    CategoryResponse hideCategory(Long id);

}