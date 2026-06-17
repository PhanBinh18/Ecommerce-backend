package com.techstore.user_service.security;


import com.techstore.user_service.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("Không tìm thấy thông tin người dùng đăng nhập!");
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}