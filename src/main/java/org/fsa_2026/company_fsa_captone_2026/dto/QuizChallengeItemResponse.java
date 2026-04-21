package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizChallengeItemResponse {
    private Integer orderIndex;
    private ChallengeBank challenge;
}
