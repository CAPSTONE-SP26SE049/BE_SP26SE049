package com.aiservice.infrastructure.services;

import com.aiservice.application.services.AIService;
import com.aiservice.application.services.PredictionService;
import com.aiservice.domain.entities.Prediction;
import com.aiservice.domain.repositories.PredictionRepository;
import com.aiservice.infrastructure.exceptions.ResourceNotFoundException;
import com.aiservice.presentation.dto.PredictionRequest;
import com.aiservice.presentation.dto.PredictionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {
    
    private final PredictionRepository predictionRepository;
    private final AIService aiService;
    
    @Override
    @Transactional
    public PredictionResponse createPrediction(PredictionRequest request) {
        log.info("Creating prediction for user ID: {}", request.getUserId());
        
        long startTime = System.currentTimeMillis();
        
        // Call AI service to get prediction
        Map<String, Object> aiResult = aiService.predict(request.getInputData());
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        // Create prediction entity
        Prediction prediction = Prediction.builder()
                .userId(request.getUserId())
                .inputData(request.getInputData().toString())
                .predictionResult(aiResult.get("result").toString())
                .modelVersion((String) aiResult.get("model_version"))
                .confidenceScore((Double) aiResult.get("confidence"))
                .processingTimeMs(processingTime)
                .build();
        
        Prediction savedPrediction = predictionRepository.save(prediction);
        log.info("Prediction created successfully with ID: {}", savedPrediction.getId());
        
        return PredictionResponse.builder()
                .id(savedPrediction.getId())
                .predictionResult(savedPrediction.getPredictionResult())
                .confidenceScore(savedPrediction.getConfidenceScore())
                .modelVersion(savedPrediction.getModelVersion())
                .processingTimeMs(savedPrediction.getProcessingTimeMs())
                .createdAt(savedPrediction.getCreatedAt())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public PredictionResponse getPredictionById(Long id) {
        log.info("Fetching prediction by ID: {}", id);
        
        Prediction prediction = predictionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prediction", "id", id));
        
        return PredictionResponse.builder()
                .id(prediction.getId())
                .predictionResult(prediction.getPredictionResult())
                .confidenceScore(prediction.getConfidenceScore())
                .modelVersion(prediction.getModelVersion())
                .processingTimeMs(prediction.getProcessingTimeMs())
                .createdAt(prediction.getCreatedAt())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PredictionResponse> getAllPredictions(Pageable pageable) {
        log.info("Fetching all predictions with pagination");
        
        return predictionRepository.findAll(pageable)
                .map(prediction -> PredictionResponse.builder()
                        .id(prediction.getId())
                        .predictionResult(prediction.getPredictionResult())
                        .confidenceScore(prediction.getConfidenceScore())
                        .modelVersion(prediction.getModelVersion())
                        .processingTimeMs(prediction.getProcessingTimeMs())
                        .createdAt(prediction.getCreatedAt())
                        .build());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<PredictionResponse> getPredictionsByUserId(Long userId, Pageable pageable) {
        log.info("Fetching predictions for user ID: {}", userId);
        
        return predictionRepository.findByUserId(userId, pageable)
                .map(prediction -> PredictionResponse.builder()
                        .id(prediction.getId())
                        .predictionResult(prediction.getPredictionResult())
                        .confidenceScore(prediction.getConfidenceScore())
                        .modelVersion(prediction.getModelVersion())
                        .processingTimeMs(prediction.getProcessingTimeMs())
                        .createdAt(prediction.getCreatedAt())
                        .build());
    }
    
    @Override
    @Transactional
    public void deletePrediction(Long id) {
        log.info("Deleting prediction with ID: {}", id);
        
        Prediction prediction = predictionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prediction", "id", id));
        
        predictionRepository.delete(prediction);
        log.info("Prediction deleted successfully with ID: {}", id);
    }
}
