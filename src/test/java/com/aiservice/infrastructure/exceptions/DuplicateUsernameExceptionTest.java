package com.aiservice.infrastructure.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateUsernameExceptionTest {

    @Test
    void testDuplicateUsernameExceptionCreation() {
        String message = "Username already exists";
        DuplicateUsernameException exception = new DuplicateUsernameException(message);

        assertEquals(message, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testDuplicateUsernameExceptionWithCause() {
        String message = "Username already exists";
        Throwable cause = new Throwable("Root cause");
        DuplicateUsernameException exception = new DuplicateUsernameException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testThrowDuplicateUsernameException() {
        assertThrows(DuplicateUsernameException.class, () -> {
            throw new DuplicateUsernameException("User exists");
        });
    }
}

