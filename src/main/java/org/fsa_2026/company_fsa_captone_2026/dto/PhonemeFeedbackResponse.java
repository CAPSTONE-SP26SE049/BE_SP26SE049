package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.AttemptPhonemeFeedback;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Phoneme Feedback Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhonemeFeedbackResponse implements Serializable {

    private String id;
    private Integer sequenceOrder;
    private String phonemeIpa;
    private BigDecimal score;
    private Integer startTimeMs;
    private Integer endTimeMs;

    public static PhonemeFeedbackResponse fromEntity(AttemptPhonemeFeedback feedback) {
        if (feedback == null)
            return null;
        return PhonemeFeedbackResponse.builder()
                .id(feedback.getId().toString())
                .sequenceOrder(feedback.getSequenceOrder())
                .phonemeIpa(feedback.getPhonemeIpa())
                .score(feedback.getScore())
                .startTimeMs(feedback.getStartTimeMs())
                .endTimeMs(feedback.getEndTimeMs())
                .build();
    }
}
