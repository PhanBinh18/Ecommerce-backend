package com.example.demo.identity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email; // Dùng email làm tài khoản đăng nhập

    @Column(nullable = false)
    private String password; // Sẽ mã hóa sau

    private String fullName;

    private String phone;

    private String address; // Địa chỉ mặc định

    // Quyền của người dùng: "ROLE_USER" hoặc "ROLE_ADMIN"
    private String role = "ROLE_USER";

    private boolean isActive = true; // Cờ khóa tài khoản

    private LocalDateTime createdAt = LocalDateTime.now();
}
