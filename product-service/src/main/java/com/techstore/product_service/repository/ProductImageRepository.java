package com.techstore.product_service.repository;

import com.techstore.product_service.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // Tìm ảnh thumbnail của product
    Optional<ProductImage> findByProductIdAndIsThumbnailTrue(Long productId);
}