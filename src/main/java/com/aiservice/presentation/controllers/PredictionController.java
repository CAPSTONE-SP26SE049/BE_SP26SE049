package com.aiservice.presentation.controllers;

import com.aiservice.application.services.AIService;
import com.aiservice.domain.entities.Prediction;
import com.aiservice.domain.repositories.PredictionRepository;
import com.aiservice.presentation.dto.PredictionRequest;
import com.aiservice.presentation.dto.PredictionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionRepository predictionRepository;
    private final AIService aiService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public PredictionResponse createPrediction(@Valid @RequestBody PredictionRequest request) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> aiResult = aiService.predict(request.getInputData());

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        Prediction prediction = Prediction.builder()
                .userId(request.getUserId())
                .inputData(request.getInputData().toString())
                .predictionResult(aiResult.get("result").toString())
                .modelVersion((String) aiResult.get("model_version"))
                .confidenceScore((Double) aiResult.get("confidence"))
                .processingTimeMs(processingTime)
                .build();

        prediction = predictionRepository.save(prediction);

        return PredictionResponse.builder()
                .id(prediction.getId())
                .predictionResult(prediction.getPredictionResult())
                .confidenceScore(prediction.getConfidenceScore())
                .modelVersion(prediction.getModelVersion())
                .processingTimeMs(prediction.getProcessingTimeMs())
                .createdAt(prediction.getCreatedAt())
                .build();
    }
}
