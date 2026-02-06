package com.aiservice.presentation.controllers;

import com.aiservice.application.services.UserService;
import com.aiservice.domain.entities.User;
import com.aiservice.presentation.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("Fetching user: {}", id);
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole())
            .isActive(user.isActive())
            .createdAt(user.getCreatedAt())
            .build());
    }

    /**
     * Get all active users
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Fetching all active users");
        List<User> users = userService.getAllActiveUsers();
        List<UserResponse> responses = users.stream()
            .map(u -> UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .role(u.getRole())
                .isActive(u.isActive())
                .createdAt(u.getCreatedAt())
                .build())
            .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Update user
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable Long id,
        @RequestBody User request) {

        log.info("Updating user: {}", id);
        request.setId(id);
        User user = userService.updateUser(request);

        return ResponseEntity.ok(UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole())
            .isActive(user.isActive())
            .createdAt(user.getCreatedAt())
            .build());
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deactivate user
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        log.info("Deactivating user: {}", id);
        User user = userService.deactivateUser(id);
        return ResponseEntity.ok(UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole())
            .isActive(user.isActive())
            .createdAt(user.getCreatedAt())
            .build());
    }
}

