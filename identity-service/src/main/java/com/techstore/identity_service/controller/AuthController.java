package com.techstore.identity_service.controller;

import com.techstore.identity_service.dto.ApiResponse;
import com.techstore.identity_service.dto.AuthResponse;
import com.techstore.identity_service.dto.LoginRequest;
import com.techstore.identity_service.dto.RegisterRequest;
import com.techstore.identity_service.service.AuthService;
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

    // Register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        AuthResponse resp = authService.register(request);
        ApiResponse<AuthResponse> envelope = ApiResponse.<AuthResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Register successful")
                .data(resp)
                .build();
        return ResponseEntity.ok(envelope);
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        AuthResponse resp = authService.login(request);
        ApiResponse<AuthResponse> envelope = ApiResponse.<AuthResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Login successful")
                .data(resp)
                .build();
        return ResponseEntity.ok(envelope);
    }

    // Google login: accept JSON { "idToken": "..." }
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@RequestBody java.util.Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            ApiResponse<AuthResponse> err = ApiResponse.<AuthResponse>builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Missing idToken in request body")
                    .build();
            return ResponseEntity.badRequest().body(err);
        }
        AuthResponse resp = authService.googleLogin(idToken);
        ApiResponse<AuthResponse> envelope = ApiResponse.<AuthResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Google login successful")
                .data(resp)
                .build();
        return ResponseEntity.ok(envelope);
    }

    // Logout: token in Authorization header "Bearer <token>"
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader HttpHeaders headers) {
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse<Void> err = ApiResponse.<Void>builder()
                    .status(HttpStatus.BAD_REQUEST.value())
                    .message("Authorization header missing or malformed")
                    .build();
            return ResponseEntity.badRequest().body(err);
        }
        String token = authHeader.substring(7);
        authService.logout(token);
        ApiResponse<Void> ok = ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Logout successful")
                .build();
        return ResponseEntity.ok(ok);
    }
}