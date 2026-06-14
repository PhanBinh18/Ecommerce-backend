package com.techstore.user_service.controller;

import com.techstore.user_service.dto.ApiResponse;
import com.techstore.user_service.entity.User;
import com.techstore.user_service.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin controller để quản lý users (list, toggle status, update role).
 * Base path: /api/v1/admin/users
 * Yêu cầu role: ADMIN
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/v1/admin/users?keyword=...
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> searchUsers(
            @RequestParam(value = "keyword", required = false) String keyword) {
        List<User> users = userService.searchUsers(keyword);
        ApiResponse<List<User>> resp = ApiResponse.<List<User>>builder()
                .status("SUCCESS")
                .message("OK")
                .data(users)
                .build();
        return ResponseEntity.ok(resp);
    }

    // PATCH /api/v1/admin/users/{id}/status
    // Toggle isActive status
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<User>> toggleUserStatus(@PathVariable Long id) {
        User updated = userService.toggleUserStatus(id);
        ApiResponse<User> resp = ApiResponse.<User>builder()
                .status("SUCCESS")
                .message("User status updated")
                .data(updated)
                .build();
        return ResponseEntity.ok(resp);
    }

    // PATCH /api/v1/admin/users/{id}/role
    // Update user role: { "roleName": "ROLE_ADMIN" }
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<User>> updateUserRole(@PathVariable Long id,
                                                            @RequestBody Map<String, String> body) {
        String roleName = body.get("roleName");
        if (roleName == null || roleName.isBlank()) {
            ApiResponse<User> err = ApiResponse.<User>builder()
                    .status("BAD_REQUEST")
                    .message("Missing roleName in request body")
                    .build();
            return ResponseEntity.badRequest().body(err);
        }

        User updated = userService.updateUserRole(id, roleName);
        ApiResponse<User> resp = ApiResponse.<User>builder()
                .status("SUCCESS")
                .message("User role updated")
                .data(updated)
                .build();
        return ResponseEntity.ok(resp);
    }
}