package com.aiservice.presentation.controllers;

import com.aiservice.domain.entities.User;
import com.aiservice.domain.repositories.UserRepository;
import com.aiservice.infrastructure.security.JwtUtils;
import com.aiservice.presentation.dto.TokenResponse;
import com.aiservice.presentation.dto.UserCreateRequest;
import com.aiservice.presentation.dto.UserLoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@RequestBody UserCreateRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : User.UserRole.PLAYER)
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody UserLoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String token = jwtUtils.generateToken(user.getUsername());

        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .build();
    }
}
