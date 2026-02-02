package com.aiservice.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "prediction_result", columnDefinition = "TEXT")
    private String predictionResult;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "confidence_score")
    private double confidenceScore;

    @Column(name = "processing_time_ms")
    private long processingTimeMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
