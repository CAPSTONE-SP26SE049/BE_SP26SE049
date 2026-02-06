package com.aiservice.infrastructure.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private String errorCode;
    private Map<String, String> errors;
    private LocalDateTime timestamp;

    public static ValidationErrorResponse of(Map<String, String> errors) {
        return ValidationErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
