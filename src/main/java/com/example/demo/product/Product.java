package com.example.demo.product;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;

    // --- CÁC TRƯỜNG MỚI THÊM ---
    private String description;
    private String imageUrl; // Sẽ dùng cho Cloudinary sau
    private String category; // Ví dụ: "LAPTOP", "DIEN_THOAI"
    private String brand; // Ví dụ: "ASUS", "APPLE"
    // Cờ đánh dấu Xóa mềm (Mặc định khi tạo mới là true - đang bán)
    @Column(nullable = false)
    private Boolean isActive = true;
}