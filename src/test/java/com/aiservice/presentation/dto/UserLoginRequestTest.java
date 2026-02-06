package com.aiservice.presentation.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserLoginRequestTest {

    @Test
    void testValidUserLoginRequest() {
        UserLoginRequest request = UserLoginRequest.builder()
            .username("testuser")
            .password("SecurePass@123")
            .build();

        assertEquals("testuser", request.getUsername());
        assertEquals("SecurePass@123", request.getPassword());
    }

    @Test
    void testUserLoginRequestNoArgsConstructor() {
        UserLoginRequest request = new UserLoginRequest();
        assertNull(request.getUsername());
        assertNull(request.getPassword());
    }

    @Test
    void testUserLoginRequestEquals() {
        UserLoginRequest request1 = new UserLoginRequest("user1", "pass1");
        UserLoginRequest request2 = new UserLoginRequest("user1", "pass1");

        assertEquals(request1, request2);
    }
}

