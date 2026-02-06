package com.aiservice.infrastructure.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void testErrorResponseCreation() {
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .errorCode("TEST_ERROR")
            .message("Test error message")
            .build();

        assertFalse(error.isSuccess());
        assertEquals("TEST_ERROR", error.getErrorCode());
        assertEquals("Test error message", error.getMessage());
        assertNotNull(error.getTimestamp());
    }

    @Test
    void testErrorResponseOf() {
        ErrorResponse error = ErrorResponse.of("VALIDATION_FAILED", "Input is invalid");

        assertFalse(error.isSuccess());
        assertEquals("VALIDATION_FAILED", error.getErrorCode());
        assertEquals("Input is invalid", error.getMessage());
        assertNotNull(error.getTimestamp());
    }

    @Test
    void testErrorResponseJsonInclude() {
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .errorCode("ERROR")
            .message("Message")
            .timestamp(null)
            .build();

        assertNotNull(error);
        assertEquals("ERROR", error.getErrorCode());
    }
}

