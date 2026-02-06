package com.aiservice.application.services;

import com.aiservice.domain.entities.User;
import com.aiservice.domain.repositories.UserRepository;
import com.aiservice.infrastructure.exceptions.DuplicateUsernameException;
import com.aiservice.infrastructure.exceptions.ResourceNotFoundException;
import com.aiservice.presentation.dto.UserCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserCreateRequest validRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validRequest = UserCreateRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("SecurePass@123")
            .role(User.UserRole.PLAYER)
            .build();

        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .passwordHash("$2a$10$encoded")
            .role(User.UserRole.PLAYER)
            .isActive(true)
            .build();
    }

    @Test
    void testRegisterUser_WithValidData_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecurePass@123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.registerUser(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertTrue(result.isActive());
    }

    @Test
    void testRegisterUser_WithDuplicateUsername_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(DuplicateUsernameException.class, () -> {
            userService.registerUser(validRequest);
        });
    }

    @Test
    void testRegisterUser_WithDuplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(DuplicateUsernameException.class, () -> {
            userService.registerUser(validRequest);
        });
    }

    @Test
    void testGetUserById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testGetUserById_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(999L);
        });
    }

    @Test
    void testGetUserByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testUsernameExists_ReturnsFalse() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        boolean exists = userService.usernameExists("nonexistent");

        // Assert
        assertFalse(exists);
    }

    @Test
    void testEmailExists_ReturnsTrue() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        boolean exists = userService.emailExists("test@example.com");

        // Assert
        assertTrue(exists);
    }
}

