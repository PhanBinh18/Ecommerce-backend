package com.example.demo.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. Dành cho User: Lấy tất cả sản phẩm ĐANG BÁN, có phân trang
    Page<Product> findAllByIsActiveTrue(Pageable pageable);

    // 2. Dành cho User: Tìm kiếm theo tên (Không phân biệt hoa thường), có phân trang
    Page<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);

    // 3. Dành cho User: Lọc theo danh mục
    Page<Product> findByCategoryAndIsActiveTrue(String category, Pageable pageable);

    // 4. Dành cho Admin: Cảnh báo sắp hết hàng (stock < mức quy định)
    List<Product> findByStockLessThanAndIsActiveTrue(Integer stock);
}