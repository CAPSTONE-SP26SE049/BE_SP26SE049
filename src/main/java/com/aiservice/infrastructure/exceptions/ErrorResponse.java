package com.aiservice.infrastructure.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private boolean success;
    private String errorCode;
    private String message;
    private LocalDateTime timestamp;

    public static ErrorResponse of(String errorCode, String message) {
        return ErrorResponse.builder()
            .success(false)
            .errorCode(errorCode)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
}

