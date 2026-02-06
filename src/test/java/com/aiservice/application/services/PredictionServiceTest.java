package com.aiservice.application.services;

import com.aiservice.domain.entities.Prediction;
import com.aiservice.domain.repositories.PredictionRepository;
import com.aiservice.infrastructure.exceptions.InvalidInputException;
import com.aiservice.infrastructure.exceptions.ResourceNotFoundException;
import com.aiservice.presentation.dto.PredictionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private PredictionService predictionService;

    private PredictionRequest validRequest;
    private Prediction testPrediction;
    private Map<String, Object> aiResult;

    @BeforeEach
    void setUp() {
        validRequest = PredictionRequest.builder()
            .userId(1L)
            .inputData(new HashMap<>(Map.of("key", "value")))
            .build();

        testPrediction = Prediction.builder()
            .id(1L)
            .userId(1L)
            .inputData("{\"key\": \"value\"}")
            .predictionResult("{\"result\": \"success\"}")
            .modelVersion("v1.0")
            .confidenceScore(0.85)
            .processingTimeMs(150)
            .build();

        aiResult = new HashMap<>();
        aiResult.put("result", "{\"class\": \"test\"}");
        aiResult.put("confidence", 0.85);
        aiResult.put("model_version", "v1.0");
    }

    @Test
    void testCreatePrediction_WithValidData_Success() {
        // Arrange
        when(aiService.predict(any())).thenReturn(aiResult);
        when(predictionRepository.save(any(Prediction.class))).thenReturn(testPrediction);

        // Act
        Prediction result = predictionService.createPrediction(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(0.85, result.getConfidenceScore());
        assertEquals("v1.0", result.getModelVersion());
    }

    @Test
    void testCreatePrediction_WithEmptyInput_ThrowsException() {
        // Arrange
        PredictionRequest invalidRequest = PredictionRequest.builder()
            .userId(1L)
            .inputData(new HashMap<>())
            .build();

        // Act & Assert
        assertThrows(InvalidInputException.class, () -> {
            predictionService.createPrediction(invalidRequest);
        });
    }

    @Test
    void testCreatePrediction_WithNullInput_ThrowsException() {
        // Arrange
        PredictionRequest invalidRequest = PredictionRequest.builder()
            .userId(1L)
            .inputData(null)
            .build();

        // Act & Assert
        assertThrows(InvalidInputException.class, () -> {
            predictionService.createPrediction(invalidRequest);
        });
    }

    @Test
    void testGetPredictionById_Success() {
        // Arrange
        when(predictionRepository.findById(1L)).thenReturn(Optional.of(testPrediction));

        // Act
        Prediction result = predictionService.getPredictionById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(0.85, result.getConfidenceScore());
    }

    @Test
    void testGetPredictionById_NotFound_ThrowsException() {
        // Arrange
        when(predictionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            predictionService.getPredictionById(999L);
        });
    }

    @Test
    void testDeletePrediction_Success() {
        // Arrange
        when(predictionRepository.existsById(1L)).thenReturn(true);

        // Act & Assert
        assertDoesNotThrow(() -> {
            predictionService.deletePrediction(1L);
        });
    }

    @Test
    void testDeletePrediction_NotFound_ThrowsException() {
        // Arrange
        when(predictionRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            predictionService.deletePrediction(999L);
        });
    }
}

