package com.techstore.product_service.repository;

import com.techstore.product_service.entity.Product;
import com.techstore.product_service.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Lấy danh sách sản phẩm active theo category id
    List<Product> findAllByStatusAndCategory_Id(ProductStatus status, Long categoryId);

    // Lấy sản phẩm có status nhất định và stockQuantity < value
    List<Product> findByStatusAndStockQuantityLessThan(ProductStatus status, Integer stock);

    // Giữ compatibility với logic cũ: tìm sản phẩm stock thấp và active
    List<Product> findByStockQuantityLessThanAndStatus(Integer stockQuantity, ProductStatus status);

    // --- ĐÃ SỬA ---
    // Bổ sung logic: Nếu là Khách Hàng (status != NULL), thì ẩn luôn các sản phẩm thuộc danh mục bị HIDDEN.
    // Nếu là Admin (status = NULL), bỏ qua kiểm tra, hiển thị tất cả.
    @Query("SELECT p FROM Product p LEFT JOIN p.brand b LEFT JOIN p.category c " +
            "WHERE (:status IS NULL OR (p.status = :status AND (c IS NULL OR c.status != com.techstore.product_service.entity.CategoryStatus.HIDDEN))) " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:category IS NULL OR :category = '' OR LOWER(c.name) = LOWER(:category)) " +
            "AND (:brand IS NULL OR :brand = '' OR LOWER(b.name) = LOWER(:brand))")
    Page<Product> searchAndFilterProducts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("brand") String brand,
            @Param("status") ProductStatus status,
            Pageable pageable);

    // Atomic Update: trừ kho nếu đủ hàng
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
            "WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int reduceStockAtomically(@Param("productId") Long productId, @Param("quantity") int quantity);
}