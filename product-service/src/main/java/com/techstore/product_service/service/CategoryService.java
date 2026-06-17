package com.techstore.product_service.service;

import com.techstore.product_service.dto.CategoryRequest;
import com.techstore.product_service.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getDisplayCategories();

    List<CategoryResponse> getAllCategoriesForAdmin();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    CategoryResponse hideCategory(Long id);

}