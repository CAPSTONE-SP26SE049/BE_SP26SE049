package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChallengeSubmissionResponse {
    private boolean correct;
    private Double score;
    private String feedback;
    private Integer bonusTokens;
    private Integer streak;
}
