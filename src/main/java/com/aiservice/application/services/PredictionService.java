package com.aiservice.application.services;

import com.aiservice.domain.entities.Prediction;
import com.aiservice.domain.repositories.PredictionRepository;
import com.aiservice.infrastructure.exceptions.InvalidInputException;
import com.aiservice.infrastructure.exceptions.ResourceNotFoundException;
import com.aiservice.presentation.dto.PredictionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final AIService aiService;

    /**
     * Create a new prediction
     */
    public Prediction createPrediction(PredictionRequest request) {
        log.info("Creating prediction for user: {}", request.getUserId());

        if (request.getInputData() == null || request.getInputData().isEmpty()) {
            throw new InvalidInputException("Input data cannot be empty");
        }

        long startTime = System.currentTimeMillis();

        // Call AI service for prediction
        Map<String, Object> aiResult = aiService.predict(request.getInputData());

        long endTime = System.currentTimeMillis();
        long processingTimeMs = endTime - startTime;

        Prediction prediction = Prediction.builder()
            .userId(request.getUserId())
            .inputData(request.getInputData().toString())
            .predictionResult(aiResult.get("result").toString())
            .modelVersion((String) aiResult.get("model_version"))
            .confidenceScore((Double) aiResult.get("confidence"))
            .processingTimeMs(processingTimeMs)
            .build();

        prediction = predictionRepository.save(prediction);
        log.info("Prediction created successfully: {}", prediction.getId());
        return prediction;
    }

    /**
     * Get prediction by ID
     */
    @Transactional(readOnly = true)
    public Prediction getPredictionById(Long id) {
        log.debug("Fetching prediction: {}", id);
        return predictionRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("Prediction not found: {}", id);
                return new ResourceNotFoundException("Prediction not found with ID: " + id);
            });
    }

    /**
     * Get all predictions for a user
     */
    @Transactional(readOnly = true)
    public List<Prediction> getUserPredictions(Long userId) {
        log.debug("Fetching predictions for user: {}", userId);
        return predictionRepository.findAll().stream()
            .filter(p -> p.getUserId().equals(userId))
            .toList();
    }

    /**
     * Get all predictions
     */
    @Transactional(readOnly = true)
    public List<Prediction> getAllPredictions() {
        log.debug("Fetching all predictions");
        return predictionRepository.findAll();
    }

    /**
     * Update prediction
     */
    public Prediction updatePrediction(Prediction prediction) {
        log.info("Updating prediction: {}", prediction.getId());

        if (!predictionRepository.existsById(prediction.getId())) {
            log.warn("Prediction not found for update: {}", prediction.getId());
            throw new ResourceNotFoundException("Prediction not found with ID: " + prediction.getId());
        }

        prediction = predictionRepository.save(prediction);
        log.info("Prediction updated: {}", prediction.getId());
        return prediction;
    }

    /**
     * Delete prediction
     */
    public void deletePrediction(Long id) {
        log.info("Deleting prediction: {}", id);

        if (!predictionRepository.existsById(id)) {
            log.warn("Prediction not found for deletion: {}", id);
            throw new ResourceNotFoundException("Prediction not found with ID: " + id);
        }

        predictionRepository.deleteById(id);
        log.info("Prediction deleted: {}", id);
    }

    /**
     * Get average confidence score
     */
    @Transactional(readOnly = true)
    public double getAverageConfidenceScore() {
        log.debug("Calculating average confidence score");
        return predictionRepository.findAll().stream()
            .mapToDouble(Prediction::getConfidenceScore)
            .average()
            .orElse(0.0);
    }

    /**
     * Get predictions with high confidence
     */
    @Transactional(readOnly = true)
    public List<Prediction> getHighConfidencePredictions(double threshold) {
        log.debug("Fetching predictions with confidence > {}", threshold);
        return predictionRepository.findAll().stream()
            .filter(p -> p.getConfidenceScore() >= threshold)
            .toList();
    }
}

