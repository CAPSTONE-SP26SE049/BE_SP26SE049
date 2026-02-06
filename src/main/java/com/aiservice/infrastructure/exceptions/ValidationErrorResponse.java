package com.aiservice.infrastructure.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorResponse {
    private boolean success;
    private String errorCode;
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp;

    public static ValidationErrorResponse of(Map<String, String> errors) {
        return ValidationErrorResponse.builder()
            .success(false)
            .errorCode("VALIDATION_FAILED")
            .message("Input validation failed")
            .errors(errors)
            .timestamp(LocalDateTime.now())
            .build();
    }
}

