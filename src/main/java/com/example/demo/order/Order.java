package com.example.demo.order;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String receiverName;    // Tên người nhận
    private String phoneNumber;     // Số điện thoại
    private String shippingAddress; // Địa chỉ giao hàng
    private String paymentMethod;   // Phương thức thanh toán

    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;
}
