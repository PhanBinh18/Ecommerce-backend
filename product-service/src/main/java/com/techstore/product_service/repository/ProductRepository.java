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

    List<Product> findAllByStatusAndCategory_Id(ProductStatus status, Long categoryId);

    List<Product> findByStatusAndStockQuantityLessThan(ProductStatus status, Integer stock);

    List<Product> findByStockQuantityLessThanAndStatus(Integer stockQuantity, ProductStatus status);

    @Query("SELECT p FROM Product p LEFT JOIN p.brand b LEFT JOIN p.category c " +
            "WHERE (:status IS NULL OR (p.status = :status AND (c IS NULL OR c.status != com.techstore.product_service.entity.CategoryStatus.HIDDEN))) " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:brandId IS NULL OR b.id = :brandId)")
    Page<Product> searchAndFilterProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("status") ProductStatus status,
            Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
            "WHERE p.id = :productId AND p.stockQuantity >= :quantity")
    int reduceStockAtomically(@Param("productId") Long productId, @Param("quantity") int quantity);
}