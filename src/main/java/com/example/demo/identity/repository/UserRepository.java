package com.example.demo.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Hàm này rất quan trọng để sau này làm chức năng Đăng nhập
    Optional<User> findByEmail(String email);

    // Kiểm tra xem email đã tồn tại khi Đăng ký chưa
    boolean existsByEmail(String email);
}
