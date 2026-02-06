package com.aiservice.infrastructure.services;

import com.aiservice.application.services.AIService;
import com.aiservice.domain.entities.Prediction;
import com.aiservice.domain.repositories.PredictionRepository;
import com.aiservice.infrastructure.exceptions.ResourceNotFoundException;
import com.aiservice.presentation.dto.PredictionRequest;
import com.aiservice.presentation.dto.PredictionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private AIService aiService;

    @InjectMocks
    private PredictionServiceImpl predictionService;

    private Prediction testPrediction;
    private PredictionRequest predictionRequest;
    private Map<String, Object> aiResult;

    @BeforeEach
    void setUp() {
        testPrediction = Prediction.builder()
                .id(1L)
                .userId(1L)
                .inputData("{\"text\":\"test input\"}")
                .predictionResult("positive")
                .modelVersion("v1.0")
                .confidenceScore(0.95)
                .processingTimeMs(150L)
                .createdAt(LocalDateTime.now())
                .build();

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("text", "test input");
        
        predictionRequest = PredictionRequest.builder()
                .userId(1L)
                .inputData(inputData)
                .build();

        aiResult = new HashMap<>();
        aiResult.put("result", "positive");
        aiResult.put("model_version", "v1.0");
        aiResult.put("confidence", 0.95);
    }

    @Test
    void createPrediction_WithValidData_ShouldCreatePrediction() {
        // Given
        when(aiService.predict(any())).thenReturn(aiResult);
        when(predictionRepository.save(any(Prediction.class))).thenReturn(testPrediction);

        // When
        PredictionResponse response = predictionService.createPrediction(predictionRequest);

        // Then
        assertNotNull(response);
        assertEquals(testPrediction.getId(), response.getId());
        assertEquals(testPrediction.getPredictionResult(), response.getPredictionResult());
        assertEquals(testPrediction.getConfidenceScore(), response.getConfidenceScore());
        verify(aiService).predict(any());
        verify(predictionRepository).save(any(Prediction.class));
    }

    @Test
    void getPredictionById_WithExistingId_ShouldReturnPrediction() {
        // Given
        when(predictionRepository.findById(1L)).thenReturn(Optional.of(testPrediction));

        // When
        PredictionResponse response = predictionService.getPredictionById(1L);

        // Then
        assertNotNull(response);
        assertEquals(testPrediction.getId(), response.getId());
        assertEquals(testPrediction.getPredictionResult(), response.getPredictionResult());
    }

    @Test
    void getPredictionById_WithNonExistingId_ShouldThrowException() {
        // Given
        when(predictionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            predictionService.getPredictionById(999L));
    }

    @Test
    void getAllPredictions_ShouldReturnPageOfPredictions() {
        // Given
        Prediction prediction2 = Prediction.builder()
                .id(2L)
                .userId(1L)
                .inputData("{\"text\":\"test 2\"}")
                .predictionResult("negative")
                .modelVersion("v1.0")
                .confidenceScore(0.85)
                .processingTimeMs(120L)
                .createdAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Prediction> predictionsPage = new PageImpl<>(
            Arrays.asList(testPrediction, prediction2), pageable, 2);
        
        when(predictionRepository.findAll(pageable)).thenReturn(predictionsPage);

        // When
        Page<PredictionResponse> responsePage = predictionService.getAllPredictions(pageable);

        // Then
        assertNotNull(responsePage);
        assertEquals(2, responsePage.getTotalElements());
        assertEquals(2, responsePage.getContent().size());
        verify(predictionRepository).findAll(pageable);
    }

    @Test
    void getPredictionsByUserId_ShouldReturnUserPredictions() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Prediction> predictionsPage = new PageImpl<>(
            Arrays.asList(testPrediction), pageable, 1);
        
        when(predictionRepository.findByUserId(1L, pageable)).thenReturn(predictionsPage);

        // When
        Page<PredictionResponse> responsePage = predictionService.getPredictionsByUserId(1L, pageable);

        // Then
        assertNotNull(responsePage);
        assertEquals(1, responsePage.getTotalElements());
        assertEquals(testPrediction.getId(), responsePage.getContent().get(0).getId());
        verify(predictionRepository).findByUserId(1L, pageable);
    }

    @Test
    void deletePrediction_WithExistingId_ShouldDeletePrediction() {
        // Given
        when(predictionRepository.findById(1L)).thenReturn(Optional.of(testPrediction));

        // When
        predictionService.deletePrediction(1L);

        // Then
        verify(predictionRepository).delete(testPrediction);
    }

    @Test
    void deletePrediction_WithNonExistingId_ShouldThrowException() {
        // Given
        when(predictionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            predictionService.deletePrediction(999L));
        verify(predictionRepository, never()).delete(any(Prediction.class));
    }
}
