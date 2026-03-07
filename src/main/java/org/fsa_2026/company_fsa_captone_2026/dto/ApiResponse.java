package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;
import java.util.Map;

/**
 * Generic API Response Wrapper
 * Matches frontend expected format:
 * { "status": "success"|"error", "message": "...", "data": {...}, "errors": {...} }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    /**
     * Response status: "success" or "error"
     */
    private String status;

    /**
     * Response message
     */
    private String message;

    /**
     * Response data payload
     */
    private T data;

    /**
     * Validation errors map (field -> error message)
     */
    private Map<String, String> errors;

    /**
     * Success response with data and message
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Success response with data only
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    /**
     * Error response with message
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .status("error")
                .message(message)
                .build();
    }

    /**
     * Error response with message and validation errors
     */
    public static <T> ApiResponse<T> error(String message, Map<String, String> errors) {
        return ApiResponse.<T>builder()
                .status("error")
                .message(message)
                .errors(errors)
                .build();
    }
}



