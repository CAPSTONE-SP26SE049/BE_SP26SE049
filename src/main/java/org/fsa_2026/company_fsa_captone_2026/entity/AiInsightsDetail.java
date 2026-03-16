package org.fsa_2026.company_fsa_captone_2026.entity;

import lombok.*;

/**
 * AiInsightsDetail
 * Structured object for Attempt.ai_insights_json
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsightsDetail {
    private Double confidenceScore;
    private String predictionResult;
    private String modelVersion;
}
