package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionResponse implements Serializable {

    private String id;
    private String skillType;
    private Integer questionOrder;
    private Integer points;
    private String challengeId;

    // Standardized fields mapped from Challenge
    private String contentText;
    private String phoneticTranscriptionIpa;
    private String referenceAudioUrl;
    private String focusPhonemes;
}