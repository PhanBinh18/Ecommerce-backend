package com.techstore.order_service.repository;

import com.techstore.order_service.entity.Order;
import com.techstore.order_service.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByIdDesc(Long userId);

    Optional<Order> findByOrderCode(String orderCode);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}