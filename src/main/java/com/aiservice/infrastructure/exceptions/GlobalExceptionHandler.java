package com.aiservice.infrastructure.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUsername(
        DuplicateUsernameException ex) {

        log.warn("Duplicate username error: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .errorCode("DUPLICATE_USERNAME")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(
        InvalidInputException ex) {

        log.warn("Invalid input error: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .errorCode("INVALID_INPUT")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
        ResourceNotFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .errorCode("RESOURCE_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
        UnauthorizedException ex) {

        log.warn("Unauthorized access: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .errorCode("UNAUTHORIZED")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
            error -> errors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation error: {}", errors);
        ValidationErrorResponse error = ValidationErrorResponse.builder()
            .success(false)
            .errorCode("VALIDATION_FAILED")
            .message("Input validation failed")
            .errors(errors)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex) {

        log.warn("Access denied: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .errorCode("ACCESS_DENIED")
            .message("You do not have permission to access this resource")
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
        Exception ex) {

        log.error("Unexpected error: ", ex);
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .errorCode("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

