package com.aiservice.infrastructure.exceptions;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationErrorResponseTest {

    @Test
    void testValidationErrorResponseCreation() {
        Map<String, String> errors = new HashMap<>();
        errors.put("email", "Email must be valid");
        errors.put("password", "Password too short");

        ValidationErrorResponse response = ValidationErrorResponse.builder()
            .success(false)
            .errorCode("VALIDATION_FAILED")
            .message("Input validation failed")
            .errors(errors)
            .build();

        assertFalse(response.isSuccess());
        assertEquals("VALIDATION_FAILED", response.getErrorCode());
        assertEquals(2, response.getErrors().size());
        assertTrue(response.getErrors().containsKey("email"));
        assertTrue(response.getErrors().containsKey("password"));
    }

    @Test
    void testValidationErrorResponseOf() {
        Map<String, String> errors = new HashMap<>();
        errors.put("username", "Username already exists");

        ValidationErrorResponse response = ValidationErrorResponse.of(errors);

        assertFalse(response.isSuccess());
        assertEquals("VALIDATION_FAILED", response.getErrorCode());
        assertEquals("Input validation failed", response.getMessage());
        assertEquals(1, response.getErrors().size());
    }

    @Test
    void testValidationErrorResponseWithNullErrors() {
        ValidationErrorResponse response = ValidationErrorResponse.builder()
            .success(false)
            .errorCode("ERROR")
            .message("Error")
            .errors(null)
            .build();

        assertFalse(response.isSuccess());
        assertNull(response.getErrors());
    }
}

