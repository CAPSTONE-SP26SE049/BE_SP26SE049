package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Prediction Entity
 * Bảng prediction - Lưu trữ kết quả dự đoán AI
 */
@Entity
@Table(name = "prediction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Prediction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Account user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "input_data", columnDefinition = "text")
    private String inputData;

    @Column(name = "prediction_result", columnDefinition = "text")
    private String predictionResult;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "model_version", length = 255)
    private String modelVersion;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
}
