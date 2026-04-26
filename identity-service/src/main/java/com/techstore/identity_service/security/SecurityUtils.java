package com.techstore.identity_service.security;


import com.techstore.identity_service.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Công cụ tiện ích giúp lấy thông tin User đang đăng nhập ở bất cứ đâu trong code.
 */
@Component
public class SecurityUtils {

    // Lấy ra toàn bộ Object User (để lấy Email, Role, ID...)
    public static User getCurrentUser() {
        // Thò tay vào két sắt lấy thông tin xác thực hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra xem có người đang đăng nhập không và thông tin đó có phải là class User của chúng ta không
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }

        // Nếu không có ai đăng nhập (Khách vãng lai), ném ra lỗi
        throw new RuntimeException("Không tìm thấy thông tin người dùng đăng nhập!");
    }

    // Hàm viết sẵn chỉ để lấy mỗi ID (Dùng nhiều nhất cho Giỏ hàng và Đơn hàng)
    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}