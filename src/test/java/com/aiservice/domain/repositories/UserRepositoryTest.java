package com.aiservice.domain.repositories;

import com.aiservice.domain.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_WithExistingUsername_ShouldReturnUser() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(User.UserRole.PLAYER)
                .isActive(true)
                .build();
        entityManager.persistAndFlush(user);

        // When
        Optional<User> found = userRepository.findByUsername("testuser");

        // Then
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void findByUsername_WithNonExistingUsername_ShouldReturnEmpty() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(User.UserRole.PLAYER)
                .isActive(true)
                .build();
        entityManager.persistAndFlush(user);

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void findByEmail_WithNonExistingEmail_ShouldReturnEmpty() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void save_WithValidUser_ShouldSaveUser() {
        // Given
        User user = User.builder()
                .username("newuser")
                .email("new@example.com")
                .passwordHash("hashedPassword")
                .role(User.UserRole.EDUCATOR)
                .isActive(true)
                .build();

        // When
        User saved = userRepository.save(user);

        // Then
        assertNotNull(saved.getId());
        assertEquals("newuser", saved.getUsername());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void delete_WithExistingUser_ShouldDeleteUser() {
        // Given
        User user = User.builder()
                .username("todelete")
                .email("delete@example.com")
                .passwordHash("hashedPassword")
                .role(User.UserRole.PLAYER)
                .isActive(true)
                .build();
        User saved = entityManager.persistAndFlush(user);

        // When
        userRepository.delete(saved);
        entityManager.flush();

        // Then
        Optional<User> found = userRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void findById_WithExistingId_ShouldReturnUser() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(User.UserRole.PLAYER)
                .isActive(true)
                .build();
        User saved = entityManager.persistAndFlush(user);

        // When
        Optional<User> found = userRepository.findById(saved.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // When
        Optional<User> found = userRepository.findById(999L);

        // Then
        assertFalse(found.isPresent());
    }
}
