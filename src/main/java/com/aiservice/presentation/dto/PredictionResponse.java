package com.aiservice.presentation.dto;

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
}
