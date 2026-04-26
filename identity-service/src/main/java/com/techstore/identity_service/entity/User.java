package com.techstore.identity_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;
    private String phone;
    private String address;
    private String role = "ROLE_USER";
    private boolean isActive = true;
    private LocalDateTime createdAt = LocalDateTime.now();

    // ========================================================
    // CÁC HÀM TÍCH HỢP VỚI SPRING SECURITY (USERDETAILS)
    // ========================================================

    // 1. Cấp quyền: Chuyển đổi biến 'role' thành đối tượng quyền mà Spring hiểu
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    // 2. Định nghĩa Username: Chúng ta dùng Email làm tài khoản đăng nhập
    @Override
    public String getUsername() {
        return email;
    }

    // 3. Tài khoản có bị hết hạn không? (Mặc định true: không hết hạn)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 4. Tài khoản có bị khóa không? (Lấy theo cờ isActive của bạn)
    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    // 5. Mật khẩu có hết hạn không? (Mặc định true)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 6. Tài khoản có đang được kích hoạt không?
    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
