package com.aiservice.domain.repositories;

import com.aiservice.domain.entities.Prediction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PredictionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PredictionRepository predictionRepository;

    @Test
    void findByUserId_WithExistingPredictions_ShouldReturnPage() {
        // Given
        Prediction prediction1 = Prediction.builder()
                .userId(1L)
                .inputData("{\"text\":\"test 1\"}")
                .predictionResult("positive")
                .modelVersion("v1.0")
                .confidenceScore(0.95)
                .processingTimeMs(100L)
                .build();
        
        Prediction prediction2 = Prediction.builder()
                .userId(1L)
                .inputData("{\"text\":\"test 2\"}")
                .predictionResult("negative")
                .modelVersion("v1.0")
                .confidenceScore(0.85)
                .processingTimeMs(120L)
                .build();
        
        Prediction prediction3 = Prediction.builder()
                .userId(2L)
                .inputData("{\"text\":\"test 3\"}")
                .predictionResult("neutral")
                .modelVersion("v1.0")
                .confidenceScore(0.75)
                .processingTimeMs(110L)
                .build();
        
        entityManager.persist(prediction1);
        entityManager.persist(prediction2);
        entityManager.persist(prediction3);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Prediction> result = predictionRepository.findByUserId(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(p -> p.getUserId().equals(1L)));
    }

    @Test
    void findByUserId_WithNonExistingUserId_ShouldReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Prediction> result = predictionRepository.findByUserId(999L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void save_WithValidPrediction_ShouldSavePrediction() {
        // Given
        Prediction prediction = Prediction.builder()
                .userId(1L)
                .inputData("{\"text\":\"test input\"}")
                .predictionResult("positive")
                .modelVersion("v1.0")
                .confidenceScore(0.95)
                .processingTimeMs(150L)
                .build();

        // When
        Prediction saved = predictionRepository.save(prediction);

        // Then
        assertNotNull(saved.getId());
        assertEquals(1L, saved.getUserId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void findById_WithExistingId_ShouldReturnPrediction() {
        // Given
        Prediction prediction = Prediction.builder()
                .userId(1L)
                .inputData("{\"text\":\"test input\"}")
                .predictionResult("positive")
                .modelVersion("v1.0")
                .confidenceScore(0.95)
                .processingTimeMs(150L)
                .build();
        Prediction saved = entityManager.persistAndFlush(prediction);

        // When
        Optional<Prediction> found = predictionRepository.findById(saved.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("positive", found.get().getPredictionResult());
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // When
        Optional<Prediction> found = predictionRepository.findById(999L);

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void delete_WithExistingPrediction_ShouldDeletePrediction() {
        // Given
        Prediction prediction = Prediction.builder()
                .userId(1L)
                .inputData("{\"text\":\"test input\"}")
                .predictionResult("positive")
                .modelVersion("v1.0")
                .confidenceScore(0.95)
                .processingTimeMs(150L)
                .build();
        Prediction saved = entityManager.persistAndFlush(prediction);

        // When
        predictionRepository.delete(saved);
        entityManager.flush();

        // Then
        Optional<Prediction> found = predictionRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_WithPagination_ShouldReturnPagedResults() {
        // Given
        for (int i = 0; i < 15; i++) {
            Prediction prediction = Prediction.builder()
                    .userId(1L)
                    .inputData("{\"text\":\"test " + i + "\"}")
                    .predictionResult("result " + i)
                    .modelVersion("v1.0")
                    .confidenceScore(0.9)
                    .processingTimeMs(100L)
                    .build();
            entityManager.persist(prediction);
        }
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Prediction> result = predictionRepository.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(15, result.getTotalElements());
        assertEquals(10, result.getContent().size());
        assertEquals(2, result.getTotalPages());
    }
}
