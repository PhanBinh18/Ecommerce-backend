package com.techstore.identity_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * DTO returned by GET /api/v1/users/profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private Boolean isActive;
    private LocalDateTime createdAt;

    /**
     * Role names, e.g. ["ROLE_ADMIN","ROLE_CUSTOMER"]
     */
    private Set<String> roles;

    /**
     * User addresses
     */
    private List<AddressResponse> addresses;
}