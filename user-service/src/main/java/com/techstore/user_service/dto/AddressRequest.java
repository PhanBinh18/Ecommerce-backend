package com.techstore.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating an address.
 * Fields: street, city, isDefault
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {
    private String street;
    private String city;

    /**
     * If true, server should mark this address as default and unset previous default(s).
     * Use Boolean to allow omission (null).
     */
    private Boolean isDefault;
}