package com.techstore.product_service.service;

import com.techstore.product_service.dto.CategoryRequest;
import com.techstore.product_service.dto.CategoryResponse;
import com.techstore.product_service.entity.Category;
import com.techstore.product_service.entity.CategoryStatus;
import com.techstore.product_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Public list: only DISPLAY categories.
     * Cached for performance.
     */
    @Cacheable(value = "categories")
    @Override
    public List<CategoryResponse> getDisplayCategories() {
        List<Category> categories = categoryRepository.findAll()
                .stream()
                .filter(c -> c.getStatus() == CategoryStatus.DISPLAY)
                .collect(Collectors.toList());
        return categories.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Admin: get all categories (no cache).
     */
    @Override
    public List<CategoryResponse> getAllCategoriesForAdmin() {
        return categoryRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        return toResponse(cat);
    }

    /**
     * Create category. Evict cache for categories list.
     */
    @Transactional
    @CacheEvict(value = {"categories", "products"}, allEntries = true)
    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        Category c = Category.builder()
                .name(request.getName())
                .status(CategoryStatus.DISPLAY)
                .build();
        Category saved = categoryRepository.save(c);
        return toResponse(saved);
    }

    /**
     * Update name. Evict cache.
     */
    @Transactional
    @CacheEvict(value = {"categories", "products"}, allEntries = true)
    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        cat.setName(request.getName());
        Category saved = categoryRepository.save(cat);
        return toResponse(saved);
    }

    /**
     * Hide category (soft delete): set status = HIDDEN. Evict cache.
     */
    @Transactional
    @CacheEvict(value = {"categories", "products"}, allEntries = true)
    @Override
    public CategoryResponse hideCategory(Long id) { // Giữ nguyên tên hàm cho đỡ phải sửa nhiều
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));

        // --- Bổ sung logic Toggle ---
        if (cat.getStatus() == CategoryStatus.HIDDEN) {
            cat.setStatus(CategoryStatus.DISPLAY); // Nếu đang ẩn thì hiện
        } else {
            cat.setStatus(CategoryStatus.HIDDEN); // Nếu đang hiện thì ẩn
        }
        // ----------------------------

        Category saved = categoryRepository.save(cat);
        return toResponse(saved);
    }

    // --- Mapping helper ---
    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .status(c.getStatus())
                .build();
    }
}