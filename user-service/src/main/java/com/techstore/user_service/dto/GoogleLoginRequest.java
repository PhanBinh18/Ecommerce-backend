package com.techstore.user_service.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String idToken;
}