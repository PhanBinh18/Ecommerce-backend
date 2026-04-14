package com.ecommerce.backend.product.repository;

import com.ecommerce.backend.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStockLessThanAndIsActiveTrue(Integer stock);

    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:category IS NULL OR :category = '' OR p.category = :category) " +
            "AND (:brand IS NULL OR :brand = '' OR p.brand = :brand)") // Bổ sung lọc theo Hãng sản xuất
    Page<Product> searchAndFilterProducts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("brand") String brand, // Bổ sung Param brand
            Pageable pageable);
}