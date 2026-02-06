package com.aiservice.presentation.dto;

import com.aiservice.domain.entities.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void userCreateRequest_WithValidData_ShouldPassValidation() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .role(User.UserRole.PLAYER)
                .build();

        // When
        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void userCreateRequest_WithInvalidEmail_ShouldFailValidation() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .email("invalid-email")
                .username("testuser")
                .password("password123")
                .build();

        // When
        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Email must be valid")));
    }

    @Test
    void userCreateRequest_WithBlankEmail_ShouldFailValidation() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .email("")
                .username("testuser")
                .password("password123")
                .build();

        // When
        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Email is required")));
    }

    @Test
    void userCreateRequest_WithShortUsername_ShouldFailValidation() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .email("test@example.com")
                .username("ab")
                .password("password123")
                .build();

        // When
        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Username must be between 3 and 50 characters")));
    }

    @Test
    void userCreateRequest_WithShortPassword_ShouldFailValidation() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("short")
                .build();

        // When
        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Password must be at least 8 characters")));
    }

    @Test
    void userLoginRequest_WithValidData_ShouldPassValidation() {
        // Given
        UserLoginRequest request = UserLoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        // When
        Set<ConstraintViolation<UserLoginRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void userLoginRequest_WithBlankUsername_ShouldFailValidation() {
        // Given
        UserLoginRequest request = UserLoginRequest.builder()
                .username("")
                .password("password123")
                .build();

        // When
        Set<ConstraintViolation<UserLoginRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Username is required")));
    }

    @Test
    void userLoginRequest_WithBlankPassword_ShouldFailValidation() {
        // Given
        UserLoginRequest request = UserLoginRequest.builder()
                .username("testuser")
                .password("")
                .build();

        // When
        Set<ConstraintViolation<UserLoginRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Password is required")));
    }

    @Test
    void predictionRequest_WithValidData_ShouldPassValidation() {
        // Given
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("text", "Hello world");
        
        PredictionRequest request = PredictionRequest.builder()
                .inputData(inputData)
                .userId(1L)
                .build();

        // When
        Set<ConstraintViolation<PredictionRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void predictionRequest_WithNullInputData_ShouldFailValidation() {
        // Given
        PredictionRequest request = PredictionRequest.builder()
                .inputData(null)
                .userId(1L)
                .build();

        // When
        Set<ConstraintViolation<PredictionRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Input data is required")));
    }

    @Test
    void predictionRequest_WithNullUserId_ShouldFailValidation() {
        // Given
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("text", "Hello world");
        
        PredictionRequest request = PredictionRequest.builder()
                .inputData(inputData)
                .userId(null)
                .build();

        // When
        Set<ConstraintViolation<PredictionRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("User ID is required")));
    }
}
