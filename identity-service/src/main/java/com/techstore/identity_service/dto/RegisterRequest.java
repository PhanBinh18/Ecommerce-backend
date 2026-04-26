package com.techstore.identity_service.dto;

import lombok.Data;

/**
 * Object hứng dữ liệu khi người dùng đăng ký tài khoản mới.
 */
@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private String address;
}