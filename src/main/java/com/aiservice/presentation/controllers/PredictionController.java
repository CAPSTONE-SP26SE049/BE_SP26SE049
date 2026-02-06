package com.aiservice.presentation.controllers;

import com.aiservice.application.services.PredictionService;
import com.aiservice.domain.entities.Prediction;
import com.aiservice.presentation.dto.PredictionRequest;
import com.aiservice.presentation.dto.PredictionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    /**
     * Create a new prediction
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PredictionResponse> createPrediction(@Valid @RequestBody PredictionRequest request) {
        log.info("Creating prediction for user: {}", request.getUserId());
        Prediction prediction = predictionService.createPrediction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(PredictionResponse.builder()
                .id(prediction.getId())
                .userId(prediction.getUserId())
                .predictionResult(prediction.getPredictionResult())
                .confidenceScore(prediction.getConfidenceScore())
                .modelVersion(prediction.getModelVersion())
                .processingTimeMs(prediction.getProcessingTimeMs())
                .createdAt(prediction.getCreatedAt())
                .build());
    }

    /**
     * Get prediction by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PredictionResponse> getPredictionById(@PathVariable Long id) {
        log.info("Fetching prediction: {}", id);
        Prediction prediction = predictionService.getPredictionById(id);
        return ResponseEntity.ok(PredictionResponse.builder()
            .id(prediction.getId())
            .userId(prediction.getUserId())
            .predictionResult(prediction.getPredictionResult())
            .confidenceScore(prediction.getConfidenceScore())
            .modelVersion(prediction.getModelVersion())
            .processingTimeMs(prediction.getProcessingTimeMs())
            .createdAt(prediction.getCreatedAt())
            .build());
    }

    /**
     * Get all predictions for a user or all predictions
     */
    @GetMapping
    public ResponseEntity<List<PredictionResponse>> getPredictions(
        @RequestParam(required = false) Long userId) {

        log.info("Fetching predictions for user: {}", userId);

        List<Prediction> predictions;
        if (userId != null) {
            predictions = predictionService.getUserPredictions(userId);
        } else {
            predictions = predictionService.getAllPredictions();
        }

        List<PredictionResponse> responses = predictions.stream()
            .map(p -> PredictionResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .predictionResult(p.getPredictionResult())
                .confidenceScore(p.getConfidenceScore())
                .modelVersion(p.getModelVersion())
                .processingTimeMs(p.getProcessingTimeMs())
                .createdAt(p.getCreatedAt())
                .build())
            .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Update prediction
     */
    @PutMapping("/{id}")
    public ResponseEntity<PredictionResponse> updatePrediction(
        @PathVariable Long id,
        @Valid @RequestBody Prediction request) {

        log.info("Updating prediction: {}", id);
        request.setId(id);
        Prediction prediction = predictionService.updatePrediction(request);

        return ResponseEntity.ok(PredictionResponse.builder()
            .id(prediction.getId())
            .userId(prediction.getUserId())
            .predictionResult(prediction.getPredictionResult())
            .confidenceScore(prediction.getConfidenceScore())
            .modelVersion(prediction.getModelVersion())
            .processingTimeMs(prediction.getProcessingTimeMs())
            .createdAt(prediction.getCreatedAt())
            .build());
    }

    /**
     * Delete prediction
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deletePrediction(@PathVariable Long id) {
        log.info("Deleting prediction: {}", id);
        predictionService.deletePrediction(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get average confidence score
     */
    @GetMapping("/stats/average-confidence")
    public ResponseEntity<Double> getAverageConfidence() {
        log.info("Fetching average confidence score");
        double avgConfidence = predictionService.getAverageConfidenceScore();
        return ResponseEntity.ok(avgConfidence);
    }
}
