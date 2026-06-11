package com.techstore.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;      // e.g. "SUCCESS" / "ERROR"
    private String message;
    private T data;
    private LocalDateTime timestamp;
}