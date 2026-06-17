package com.techstore.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


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
    private Set<String> roles;
    private List<AddressResponse> addresses;
}