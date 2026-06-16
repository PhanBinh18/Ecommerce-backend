package com.techstore.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String receiverName;
    private String phoneNumber;
    private String shippingAddress;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private Double latitude;
    private Double longitude;
}