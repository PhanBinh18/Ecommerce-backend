package com.techstore.user_service.controller;

import com.techstore.user_service.dto.*;
import com.techstore.user_service.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller (register/login/google/logout)
 */
@RestController
@RequestMapping("/api/v1/users/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        AuthResponse resp = authService.register(request);
        ApiResponse<AuthResponse> envelope = ApiResponse.<AuthResponse>builder()
                .status("SUCCESS")
                .message("Register successful")
                .data(resp)
                .build();
        return ResponseEntity.ok(envelope);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        AuthResponse resp = authService.login(request);
        ApiResponse<AuthResponse> envelope = ApiResponse.<AuthResponse>builder()
                .status("SUCCESS")
                .message("Login successful")
                .data(resp)
                .build();
        return ResponseEntity.ok(envelope);
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        try {
            AuthResponse response = authService.googleLogin(request.getIdToken());
            return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                    .status("SUCCESS")
                    .message("Đăng nhập Google thành công")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<AuthResponse>builder()
                    .status("ERROR")
                    .message("Xác thực Google thất bại: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader HttpHeaders headers) {
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse<Void> err = ApiResponse.<Void>builder()
                    .status("BAD_REQUEST")
                    .message("Authorization header missing or malformed")
                    .build();
            return ResponseEntity.badRequest().body(err);
        }
        String token = authHeader.substring(7);
        authService.logout(token);
        ApiResponse<Void> ok = ApiResponse.<Void>builder()
                .status("SUCCESS")
                .message("Logout successful")
                .build();
        return ResponseEntity.ok(ok);
    }
}