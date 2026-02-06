package com.aiservice.presentation.controllers;

import com.aiservice.application.services.UserService;
import com.aiservice.domain.entities.User;
import com.aiservice.infrastructure.security.JwtUtils;
import com.aiservice.presentation.dto.TokenResponse;
import com.aiservice.presentation.dto.UserCreateRequest;
import com.aiservice.presentation.dto.UserLoginRequest;
import com.aiservice.presentation.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserCreateRequest request) {
        log.info("User registration request for username: {}", request.getUsername());
        return userService.createUser(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody UserLoginRequest request) {
        log.info("User login request for username: {}", request.getUsername());
        
        User user = userService.authenticateUser(request.getUsername(), request.getPassword());
        String token = jwtUtils.generateToken(user.getUsername());

        return TokenResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .build();
    }
}
