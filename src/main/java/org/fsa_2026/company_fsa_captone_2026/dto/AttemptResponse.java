package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Attempt;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Attempt Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptResponse implements Serializable {

    private String id;
    private String sessionId;
    private String challengeId;
    private String audioUrl;
    private BigDecimal scoreOverall;
    private Boolean isPassed;
    private Integer latencyMs;
    private Instant createdAt;

    // Optional list of feedback
    private List<PhonemeFeedbackResponse> feedback;

    public static AttemptResponse fromEntity(Attempt attempt) {
        if (attempt == null)
            return null;
        return AttemptResponse.builder()
                .id(attempt.getId().toString())
                .sessionId(attempt.getSession() != null ? attempt.getSession().getId().toString() : null)
                .challengeId(attempt.getChallenge() != null ? attempt.getChallenge().getId().toString() : null)
                .audioUrl(attempt.getAudioUrl())
                .scoreOverall(attempt.getScoreOverall())
                .isPassed(attempt.getIsPassed())
                .latencyMs(attempt.getLatencyMs())
                .createdAt(attempt.getCreatedAt())
                .build();
    }
}
