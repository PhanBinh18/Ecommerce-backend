package com.techstore.product_service.entity; // Thay đổi lại package cho đúng dự án của bạn

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "brands")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "products") // Tránh lỗi vòng lặp vô hạn khi log dữ liệu
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 20)
    private String status = "ACTIVE"; // Mặc định là ACTIVE, có thể chuyển thành HIDDEN nếu muốn ẩn hãng đó

    // Quan hệ 1-N với Product: Một thương hiệu có nhiều sản phẩm
    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;
}