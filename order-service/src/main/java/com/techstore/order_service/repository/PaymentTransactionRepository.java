package com.techstore.order_service.repository;

import com.techstore.order_service.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    // Tìm tất cả payment transaction thuộc 1 order (Spring Data sẽ dịch orderId -> order.id)
    List<PaymentTransaction> findByOrderId(Long orderId);

    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);
}