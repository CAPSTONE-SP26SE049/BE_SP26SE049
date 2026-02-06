package com.aiservice.domain.repositories;

import com.aiservice.domain.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("dev")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .username("testuser")
            .email("test@example.com")
            .passwordHash("$2a$10$encoded")
            .role(User.UserRole.PLAYER)
            .isActive(true)
            .build();

        userRepository.save(testUser);
    }

    @Test
    void testFindByUsername_ReturnsUser() {
        // Act
        Optional<User> found = userRepository.findByUsername("testuser");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void testFindByUsername_NotFound_ReturnsEmpty() {
        // Act
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void testFindByEmail_ReturnsUser() {
        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void testFindByEmail_NotFound_ReturnsEmpty() {
        // Act
        Optional<User> found = userRepository.findByEmail("notfound@example.com");

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void testSave_CreatesUser() {
        // Arrange
        User newUser = User.builder()
            .username("newuser")
            .email("new@example.com")
            .passwordHash("$2a$10$encoded")
            .role(User.UserRole.EDUCATOR)
            .isActive(true)
            .build();

        // Act
        User saved = userRepository.save(newUser);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("newuser", saved.getUsername());
        assertEquals(User.UserRole.EDUCATOR, saved.getRole());
    }

    @Test
    void testUpdate_ModifiesUser() {
        // Arrange
        testUser.setEmail("updated@example.com");

        // Act
        User updated = userRepository.save(testUser);

        // Assert
        assertEquals("updated@example.com", updated.getEmail());
    }

    @Test
    void testDelete_RemovesUser() {
        // Arrange
        Long userId = testUser.getId();

        // Act
        userRepository.deleteById(userId);

        // Assert
        Optional<User> found = userRepository.findById(userId);
        assertTrue(found.isEmpty());
    }

    @Test
    void testFindByUsername_CaseSensitive() {
        // Act
        Optional<User> found = userRepository.findByUsername("TESTUSER");

        // Assert - Should not find due to case sensitivity
        assertTrue(found.isEmpty());
    }
}

