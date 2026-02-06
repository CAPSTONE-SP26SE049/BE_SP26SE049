package com.aiservice.application.services;

import com.aiservice.domain.entities.User;
import com.aiservice.domain.repositories.UserRepository;
import com.aiservice.infrastructure.exceptions.DuplicateUsernameException;
import com.aiservice.infrastructure.exceptions.ResourceNotFoundException;
import com.aiservice.presentation.dto.UserCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user
     */
    public User registerUser(UserCreateRequest request) {
        log.info("Registering user: {}", request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed - duplicate username: {}", request.getUsername());
            throw new DuplicateUsernameException("Username already exists");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed - duplicate email: {}", request.getEmail());
            throw new DuplicateUsernameException("Email already exists");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(request.getRole() != null ? request.getRole() : User.UserRole.PLAYER)
            .isActive(true)
            .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getId());
        return user;
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("User not found: {}", id);
                return new ResourceNotFoundException("User not found with ID: " + id);
            });
    }

    /**
     * Get user by username
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.warn("User not found: {}", username);
                return new ResourceNotFoundException("User not found with username: " + username);
            });
    }

    /**
     * Get all active users
     */
    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        log.debug("Fetching all active users");
        return userRepository.findAll().stream()
            .filter(User::isActive)
            .toList();
    }

    /**
     * Update user
     */
    public User updateUser(User user) {
        log.info("Updating user: {}", user.getId());

        if (!userRepository.existsById(user.getId())) {
            log.warn("User not found for update: {}", user.getId());
            throw new ResourceNotFoundException("User not found with ID: " + user.getId());
        }

        user = userRepository.save(user);
        log.info("User updated successfully: {}", user.getId());
        return user;
    }

    /**
     * Delete user
     */
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);

        if (!userRepository.existsById(id)) {
            log.warn("User not found for deletion: {}", id);
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully: {}", id);
    }

    /**
     * Deactivate user (soft delete)
     */
    public User deactivateUser(Long id) {
        log.info("Deactivating user: {}", id);
        User user = getUserById(id);
        user.setActive(false);
        user = userRepository.save(user);
        log.info("User deactivated: {}", id);
        return user;
    }

    /**
     * Check if username exists
     */
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    /**
     * Check if email exists
     */
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}

