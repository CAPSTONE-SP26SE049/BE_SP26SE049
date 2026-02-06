package com.aiservice.presentation.controllers;

import com.aiservice.domain.entities.User;
import com.aiservice.domain.repositories.UserRepository;
import com.aiservice.infrastructure.exceptions.DuplicateUsernameException;
import com.aiservice.infrastructure.security.JwtUtils;
import com.aiservice.presentation.dto.TokenResponse;
import com.aiservice.presentation.dto.UserCreateRequest;
import com.aiservice.presentation.dto.UserLoginRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserRegisterResponse> register(@Valid @RequestBody UserCreateRequest request) {
        log.info("Registration attempt for username: {}", request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new DuplicateUsernameException("Username already exists");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
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
        log.info("User registered successfully: {}", user.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UserRegisterResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody UserLoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", request.getUsername());
                    return new com.aiservice.infrastructure.exceptions.UnauthorizedException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed - invalid password for user: {}", request.getUsername());
            throw new com.aiservice.infrastructure.exceptions.UnauthorizedException("Invalid credentials");
        }

        String token = jwtUtils.generateToken(user.getUsername());
        log.info("User logged in successfully: {}", user.getUsername());

        return ResponseEntity.ok(TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserRegisterResponse {
        private Long id;
        private String username;
        private String email;
        private User.UserRole role;
    }
}
