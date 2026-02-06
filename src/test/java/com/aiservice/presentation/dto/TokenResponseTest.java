package com.aiservice.presentation.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenResponseTest {

    @Test
    void testTokenResponseCreation() {
        TokenResponse response = TokenResponse.builder()
            .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            .tokenType("Bearer")
            .expiresIn(86400L)
            .build();

        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresIn());
    }

    @Test
    void testTokenResponseNoArgsConstructor() {
        TokenResponse response = new TokenResponse();
        assertNull(response.getAccessToken());
        assertNull(response.getTokenType());
        assertNull(response.getExpiresIn());
    }

    @Test
    void testTokenResponseAllArgsConstructor() {
        TokenResponse response = new TokenResponse(
            "token-string",
            "Bearer",
            86400L
        );

        assertEquals("token-string", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresIn());
    }
}

