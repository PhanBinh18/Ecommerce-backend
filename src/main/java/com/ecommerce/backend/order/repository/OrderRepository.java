package com.ecommerce.backend.order.repository;

import com.ecommerce.backend.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Tự động generate câu SQL: SELECT * FROM orders WHERE user_id = ? ORDER BY id DESC
    List<Order> findByUserIdOrderByIdDesc(Long userId);
}