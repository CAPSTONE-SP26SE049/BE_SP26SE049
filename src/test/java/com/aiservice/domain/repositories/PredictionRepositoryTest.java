package com.aiservice.domain.repositories;

import com.aiservice.domain.entities.Prediction;
import com.aiservice.domain.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("dev")
class PredictionRepositoryTest {

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Prediction testPrediction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .username("testuser")
            .email("test@example.com")
            .passwordHash("$2a$10$encoded")
            .role(User.UserRole.PLAYER)
            .isActive(true)
            .build();
        testUser = userRepository.save(testUser);

        testPrediction = Prediction.builder()
            .userId(testUser.getId())
            .inputData("{\"data\": \"test\"}")
            .predictionResult("{\"result\": \"success\"}")
            .modelVersion("v1.0")
            .confidenceScore(0.85)
            .processingTimeMs(150)
            .build();

        predictionRepository.save(testPrediction);
    }

    @Test
    void testSave_CreatesPrediction() {
        // Arrange
        Prediction newPrediction = Prediction.builder()
            .userId(testUser.getId())
            .inputData("{\"data\": \"new\"}")
            .predictionResult("{\"result\": \"new\"}")
            .modelVersion("v1.0")
            .confidenceScore(0.90)
            .processingTimeMs(200)
            .build();

        // Act
        Prediction saved = predictionRepository.save(newPrediction);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(testUser.getId(), saved.getUserId());
        assertEquals(0.90, saved.getConfidenceScore());
    }

    @Test
    void testSave_SetsCreatedAt() {
        // Act
        Prediction prediction = predictionRepository.findById(testPrediction.getId()).orElse(null);

        // Assert
        assertNotNull(prediction);
        assertNotNull(prediction.getCreatedAt());
        assertTrue(prediction.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testFindById_ReturnsPrediction() {
        // Act
        Optional<Prediction> found = predictionRepository.findById(testPrediction.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(testPrediction.getId(), found.get().getId());
        assertEquals(0.85, found.get().getConfidenceScore());
    }

    @Test
    void testFindById_NotFound_ReturnsEmpty() {
        // Act
        Optional<Prediction> found = predictionRepository.findById(999L);

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void testUpdate_ModifiesPrediction() {
        // Arrange
        testPrediction.setConfidenceScore(0.95);

        // Act
        Prediction updated = predictionRepository.save(testPrediction);

        // Assert
        assertEquals(0.95, updated.getConfidenceScore());
    }

    @Test
    void testDelete_RemovesPrediction() {
        // Arrange
        Long predictionId = testPrediction.getId();

        // Act
        predictionRepository.deleteById(predictionId);

        // Assert
        Optional<Prediction> found = predictionRepository.findById(predictionId);
        assertTrue(found.isEmpty());
    }

    @Test
    void testFindAll_ReturnsPredictions() {
        // Act
        List<Prediction> predictions = predictionRepository.findAll();

        // Assert
        assertTrue(predictions.size() >= 1);
    }

    @Test
    void testConfidenceScoreRange() {
        // Assert - Verify confidence score is between 0 and 1
        Prediction prediction = predictionRepository.findById(testPrediction.getId()).orElse(null);
        assertNotNull(prediction);
        assertTrue(prediction.getConfidenceScore() >= 0);
        assertTrue(prediction.getConfidenceScore() <= 1);
    }
}

