package com.techstore.identity_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response envelope:
 * {
 *   "status": 200,
 *   "message": "OK",
 *   "data": { ... }
 * }
 *
 * T is the payload type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    /**
     * HTTP-like status code (e.g., 200, 400, 401, 500)
     */
    private int status;

    /**
     * Human readable message
     */
    private String message;

    /**
     * The actual payload (can be null)
     */
    private T data;
}