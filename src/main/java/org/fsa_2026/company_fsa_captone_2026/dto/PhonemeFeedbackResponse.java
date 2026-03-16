package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


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


    public static PhonemeFeedbackResponse fromDetail(org.fsa_2026.company_fsa_captone_2026.entity.PhonemeFeedbackDetail detail) {
        if (detail == null)
            return null;
        return PhonemeFeedbackResponse.builder()
                .sequenceOrder(detail.getSequenceOrder())
                .phonemeIpa(detail.getPhonemeIpa())
                .score(detail.getScore())
                .startTimeMs(detail.getStartTimeMs())
                .endTimeMs(detail.getEndTimeMs())
                .build();
    }
}
