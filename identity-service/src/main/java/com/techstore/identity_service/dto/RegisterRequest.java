package com.techstore.identity_service.dto;

import lombok.Data;

/**
 * Request payload when a user registers.
 * Spec: email, password, fullName, phone
 */
@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String phone;
}