package com.techstore.cart_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@Data
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    @JsonIgnore
    private Cart cart;
    private Long productId;
    private Integer quantity;
    // Snapshot fields
    private String productName;
    private String thumbnailUrl;
    @Column(precision = 19, scale = 2)
    private BigDecimal price;
    @Column(precision = 19, scale = 2)
    private BigDecimal subTotal;
}