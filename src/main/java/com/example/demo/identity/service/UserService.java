package com.example.demo.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Đăng ký User mới
    public User createUser(User user) {
        // Kiểm tra trùng email
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }
        // Tạm thời bỏ qua mã hóa password để test luồng
        return userRepository.save(user);
    }

    // Lấy thông tin User theo ID (dùng cho các module khác gọi sang sau này)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + id));
    }

    // Lấy danh sách toàn bộ User (Cho Admin)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
