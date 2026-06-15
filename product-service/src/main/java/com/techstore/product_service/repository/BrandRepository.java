package com.techstore.product_service.repository;

import com.techstore.product_service.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    // Lấy danh sách các thương hiệu đang ACTIVE để hiển thị lên Dropdown ở Frontend
    List<Brand> findByStatus(String status);
}