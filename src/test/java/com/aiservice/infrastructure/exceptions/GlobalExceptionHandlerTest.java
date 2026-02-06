package com.aiservice.infrastructure.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleDuplicateUsername_ShouldReturnBadRequest() {
        // Given
        DuplicateUsernameException exception = new DuplicateUsernameException("Username already exists");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateUsername(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DUPLICATE_USERNAME", response.getBody().getErrorCode());
        assertEquals("Username already exists", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleInvalidInput_ShouldReturnBadRequest() {
        // Given
        InvalidInputException exception = new InvalidInputException("Invalid input provided");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleInvalidInput(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_INPUT", response.getBody().getErrorCode());
        assertEquals("Invalid input provided", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorized_ShouldReturnUnauthorized() {
        // Given
        UnauthorizedException exception = new UnauthorizedException("Invalid credentials");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(exception);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNAUTHORIZED", response.getBody().getErrorCode());
        assertEquals("Invalid credentials", response.getBody().getMessage());
    }

    @Test
    void handleValidationErrors_ShouldReturnValidationErrorResponse() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("userCreateRequest", "username", "Username is required");
        FieldError fieldError2 = new FieldError("userCreateRequest", "email", "Email must be valid");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<ValidationErrorResponse> response = handler.handleValidationErrors(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
        
        Map<String, String> errors = response.getBody().getErrors();
        assertEquals(2, errors.size());
        assertEquals("Username is required", errors.get("username"));
        assertEquals("Email must be valid", errors.get("email"));
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        Exception exception = new Exception("Unexpected error");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }
}
