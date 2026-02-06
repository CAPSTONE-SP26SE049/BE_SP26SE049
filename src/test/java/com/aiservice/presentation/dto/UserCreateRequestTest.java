package com.aiservice.presentation.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserCreateRequestTest {

    @Test
    void testValidUserCreateRequest() {
        UserCreateRequest request = UserCreateRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("SecurePass@123")
            .build();

        assertEquals("testuser", request.getUsername());
        assertEquals("test@example.com", request.getEmail());
        assertEquals("SecurePass@123", request.getPassword());
    }

    @Test
    void testUserCreateRequestWithRole() {
        com.aiservice.domain.entities.User.UserRole role = com.aiservice.domain.entities.User.UserRole.EDUCATOR;
        UserCreateRequest request = UserCreateRequest.builder()
            .username("teacher")
            .email("teacher@example.com")
            .password("TeacherPass@456")
            .role(role)
            .build();

        assertEquals(role, request.getRole());
    }

    @Test
    void testUserCreateRequestNoArgsConstructor() {
        UserCreateRequest request = new UserCreateRequest();
        assertNull(request.getUsername());
        assertNull(request.getEmail());
    }
}

