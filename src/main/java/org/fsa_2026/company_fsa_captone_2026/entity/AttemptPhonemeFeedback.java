package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * AttemptPhonemeFeedback Entity (NEW - ISO Compliant)
 * Table: attempt_phoneme_feedback
 * Normalized per-phoneme feedback (replaces ai_analysis_json)
 */
@Entity
@Table(name = "attempt_phoneme_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptPhonemeFeedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "phoneme_ipa", nullable = false, length = 10)
    private String phonemeIpa;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "start_time_ms")
    private Integer startTimeMs;

    @Column(name = "end_time_ms")
    private Integer endTimeMs;
}

