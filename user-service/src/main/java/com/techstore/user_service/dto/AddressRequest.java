package com.techstore.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {
    private String receiverName;
    private String phoneNumber;
    private String shippingAddress;
    private Boolean isDefault;
}