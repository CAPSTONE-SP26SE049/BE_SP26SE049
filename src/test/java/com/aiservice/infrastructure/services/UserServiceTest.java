package com.aiservice.infrastructure.services;

import com.aiservice.domain.entities.User;
import com.aiservice.domain.repositories.UserRepository;
import com.aiservice.infrastructure.exceptions.DuplicateUsernameException;
import com.aiservice.infrastructure.exceptions.ResourceNotFoundException;
import com.aiservice.infrastructure.exceptions.UnauthorizedException;
import com.aiservice.presentation.dto.UserCreateRequest;
import com.aiservice.presentation.dto.UserResponse;
import com.aiservice.presentation.dto.UserUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(User.UserRole.PLAYER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = UserCreateRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .role(User.UserRole.PLAYER)
                .build();
    }

    @Test
    void createUser_WithValidData_ShouldCreateUser() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.createUser(createRequest);

        // Then
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getUsername(), response.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithDuplicateUsername_ShouldThrowException() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(DuplicateUsernameException.class, () -> userService.createUser(createRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldThrowException() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(DuplicateUsernameException.class, () -> userService.createUser(createRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateUser_WithValidCredentials_ShouldReturnUser() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When
        User result = userService.authenticateUser("testuser", "password123");

        // Then
        assertNotNull(result);
        assertEquals(testUser.getUsername(), result.getUsername());
    }

    @Test
    void authenticateUser_WithInvalidUsername_ShouldThrowException() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            userService.authenticateUser("wronguser", "password123"));
    }

    @Test
    void authenticateUser_WithInvalidPassword_ShouldThrowException() {
        // Given
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            userService.authenticateUser("testuser", "wrongpassword"));
    }

    @Test
    void authenticateUser_WithInactiveUser_ShouldThrowException() {
        // Given
        testUser.setActive(false);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            userService.authenticateUser("testuser", "password123"));
    }

    @Test
    void getUserById_WithExistingId_ShouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserById(1L);

        // Then
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getUsername(), response.getUsername());
    }

    @Test
    void getUserById_WithNonExistingId_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    void getUserByUsername_WithExistingUsername_ShouldReturnUser() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserByUsername("testuser");

        // Then
        assertNotNull(response);
        assertEquals(testUser.getUsername(), response.getUsername());
    }

    @Test
    void getUserByUsername_WithNonExistingUsername_ShouldThrowException() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            userService.getUserByUsername("nonexistent"));
    }

    @Test
    void updateUser_WithValidData_ShouldUpdateUser() {
        // Given
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .email("updated@example.com")
                .role(User.UserRole.EDUCATOR)
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.updateUser(1L, updateRequest);

        // Then
        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_WithNonExistingId_ShouldThrowException() {
        // Given
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .email("updated@example.com")
                .build();
        
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            userService.updateUser(999L, updateRequest));
    }

    @Test
    void deleteUser_WithExistingId_ShouldDeleteUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_WithNonExistingId_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(999L));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Given
        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .passwordHash("hash")
                .role(User.UserRole.PLAYER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

        // When
        List<UserResponse> responses = userService.getAllUsers();

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(userRepository).findAll();
    }
}
