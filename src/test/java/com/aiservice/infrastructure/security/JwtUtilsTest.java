package com.aiservice.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // Set test values using reflection
        ReflectionTestUtils.setField(jwtUtils, "secret", "8520af2bad412296716a445f62decdcb59c7336b13cf14a51e6b8c6e267d3e26");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 86400000L);
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        // Given
        String username = "testuser";

        // When
        String token = jwtUtils.generateToken(username);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void getUsernameFromToken_ShouldReturnCorrectUsername() {
        // Given
        String username = "testuser";
        String token = jwtUtils.generateToken(username);

        // When
        String extractedUsername = jwtUtils.getUsernameFromToken(token);

        // Then
        assertEquals(username, extractedUsername);
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String username = "testuser";
        String token = jwtUtils.generateToken(username);

        // When
        Boolean isValid = jwtUtils.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_WithValidTokenAndUsername_ShouldReturnTrue() {
        // Given
        String username = "testuser";
        String token = jwtUtils.generateToken(username);

        // When
        Boolean isValid = jwtUtils.validateToken(token, username);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_WithInvalidUsername_ShouldReturnFalse() {
        // Given
        String username = "testuser";
        String token = jwtUtils.generateToken(username);

        // When
        Boolean isValid = jwtUtils.validateToken(token, "wronguser");

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        Boolean isValid = jwtUtils.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        // Given
        JwtUtils shortExpirationJwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(shortExpirationJwtUtils, "secret", "8520af2bad412296716a445f62decdcb59c7336b13cf14a51e6b8c6e267d3e26");
        ReflectionTestUtils.setField(shortExpirationJwtUtils, "expiration", -1000L); // Already expired
        
        String username = "testuser";
        String token = shortExpirationJwtUtils.generateToken(username);

        // When
        Boolean isValid = jwtUtils.validateToken(token);

        // Then
        assertFalse(isValid);
    }
}
