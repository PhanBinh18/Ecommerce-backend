package com.techstore.user_service.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    // Frontend (React) sẽ lấy được một chuỗi mã hóa dài từ Google và gửi vào biến này
    private String idToken;
}