package com.aiservice.presentation.controllers;

import com.aiservice.domain.entities.User;
import com.aiservice.domain.repositories.UserRepository;
import com.aiservice.infrastructure.exceptions.DuplicateUsernameException;
import com.aiservice.infrastructure.security.JwtUtils;
import com.aiservice.presentation.dto.UserCreateRequest;
import com.aiservice.presentation.dto.UserLoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtils jwtUtils;

    private UserCreateRequest validRegisterRequest;
    private UserLoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        validRegisterRequest = UserCreateRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("SecurePass@123")
            .role(User.UserRole.PLAYER)
            .build();

        validLoginRequest = UserLoginRequest.builder()
            .username("testuser")
            .password("SecurePass@123")
            .build();
    }

    @Test
    void testRegister_WithValidData_Returns201() throws Exception {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecurePass@123")).thenReturn("$2a$10$encoded");

        User savedUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .role(User.UserRole.PLAYER)
            .isActive(true)
            .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testRegister_WithDuplicateUsername_ReturnsBadRequest() throws Exception {
        // Arrange
        when(userRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(new User()));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("DUPLICATE_USERNAME"));
    }

    @Test
    void testRegister_WithDuplicateEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(new User()));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("DUPLICATE_USERNAME"));
    }

    @Test
    void testRegister_WithInvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        UserCreateRequest invalidRequest = UserCreateRequest.builder()
            .username("testuser")
            .email("invalid-email")
            .password("SecurePass@123")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void testRegister_WithWeakPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        UserCreateRequest invalidRequest = UserCreateRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("weak")  // Too short, no special chars
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void testLogin_WithValidCredentials_ReturnsToken() throws Exception {
        // Arrange
        User user = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .passwordHash("$2a$10$encoded")
            .role(User.UserRole.PLAYER)
            .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePass@123", "$2a$10$encoded")).thenReturn(true);
        when(jwtUtils.generateToken("testuser")).thenReturn("jwt-token-here");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-token-here"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    @Test
    void testLogin_WithInvalidUsername_ReturnsUnauthorized() throws Exception {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void testLogin_WithInvalidPassword_ReturnsUnauthorized() throws Exception {
        // Arrange
        User user = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .passwordHash("$2a$10$encoded")
            .role(User.UserRole.PLAYER)
            .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePass@123", "$2a$10$encoded")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void testLogin_WithBlankUsername_ReturnsBadRequest() throws Exception {
        // Arrange
        UserLoginRequest invalidRequest = UserLoginRequest.builder()
            .username("")
            .password("SecurePass@123")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}

