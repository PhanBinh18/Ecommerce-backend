package com.techstore.user_service.controller;

import com.techstore.user_service.dto.ApiResponse;
import com.techstore.user_service.dto.UserProfileResponse;
import com.techstore.user_service.dto.UserProfileUpdateRequest;
import com.techstore.user_service.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        ApiResponse<UserProfileResponse> resp = ApiResponse.<UserProfileResponse>builder()
                .status("SUCCESS")
                .message("OK")
                .data(profile)
                .build();
        return ResponseEntity.ok(resp);
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @RequestBody UserProfileUpdateRequest request) {
        UserProfileResponse updated = userService.updateCurrentUserProfile(
                request.getFullName(),
                request.getPhone()
        );
        ApiResponse<UserProfileResponse> resp = ApiResponse.<UserProfileResponse>builder()
                .status("SUCCESS")
                .message("Profile updated")
                .data(updated)
                .build();
        return ResponseEntity.ok(resp);
    }
}