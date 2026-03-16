package org.fsa_2026.company_fsa_captone_2026.entity;

import lombok.*;

import java.math.BigDecimal;

/**
 * PhonemeFeedbackDetail
 * Structured object for Attempt.phoneme_feedback_json
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhonemeFeedbackDetail {
    private Integer sequenceOrder;
    private String phonemeIpa;
    private BigDecimal score;
    private Integer startTimeMs;
    private Integer endTimeMs;
}
