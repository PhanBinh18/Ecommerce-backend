package com.techstore.user_service.repository;

import com.techstore.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data JPA sẽ tự động sinh mã SQL: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Kiểm tra xem email đã tồn tại khi Đăng ký chưa
    boolean existsByEmail(String email);
}