package com.aiservice.presentation.dto;

import com.aiservice.domain.entities.Prediction;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResponse {
    private Long id;
    private String predictionResult;
    private double confidenceScore;
    private String modelVersion;
    private long processingTimeMs;
    private LocalDateTime createdAt;

    public static PredictionResponse fromEntity(Prediction prediction) {
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
