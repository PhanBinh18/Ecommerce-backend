package com.techstore.identity_service.controller;

import com.techstore.identity_service.dto.ApiResponse;
import com.techstore.identity_service.dto.UserProfileResponse;
import com.techstore.identity_service.dto.UserProfileUpdateRequest;
import com.techstore.identity_service.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller để quản lý profile của user hiện tại.
 * Base path: /api/v1/users/profile
 * Yêu cầu: authentication (token JWT hợp lệ)
 */
@RestController
@RequestMapping("/api/v1/users/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/v1/users/profile
     * Lấy thông tin profile của user hiện đang đăng nhập.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        ApiResponse<UserProfileResponse> resp = ApiResponse.<UserProfileResponse>builder()
                .status(HttpStatus.OK.value())
                .message("OK")
                .data(profile)
                .build();
        return ResponseEntity.ok(resp);
    }

    /**
     * PUT /api/v1/users/profile
     * Cập nhật thông tin profile (fullName, phone) của user hiện đang đăng nhập.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @RequestBody UserProfileUpdateRequest request) {
        UserProfileResponse updated = userService.updateCurrentUserProfile(
                request.getFullName(),
                request.getPhone()
        );
        ApiResponse<UserProfileResponse> resp = ApiResponse.<UserProfileResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Profile updated")
                .data(updated)
                .build();
        return ResponseEntity.ok(resp);
    }
}