package com.techstore.order_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many PaymentTransaction -> One Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(unique = true)
    private String transactionCode;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    private String status;        // ví dụ: PENDING, SUCCESS, FAILED
    private String paymentMethod; // VNPay, COD, MOMO...
    private String responseCode;  // code trả về từ cổng thanh toán
    @Column(length = 1000)
    private String message;       // thông tin bổ sung

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}