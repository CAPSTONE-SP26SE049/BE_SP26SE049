package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizQuestion;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionResponse implements Serializable {

    private String id;
    private String skillType;
    private String difficulty;
    private Integer questionOrder;
    private Integer points;
    private String challengeId;

    // Standardized fields mapped from Challenge
    private String contentText;
    private String phoneticTranscriptionIpa;
    private String referenceAudioUrl;
    private String focusPhonemes;

    public static QuizQuestionResponse fromEntity(QuizQuestion entity) {
        if (entity == null) return null;
        QuizQuestionResponse response = QuizQuestionResponse.builder()
                .id(entity.getId().toString())
                .skillType(entity.getSkillType())
                .difficulty(entity.getDifficulty() != null ? entity.getDifficulty().name() : null)
                .questionOrder(entity.getQuestionOrder())
                .points(entity.getPoints())
                .build();

        if (entity.getChallenge() != null) {
            response.setChallengeId(entity.getChallenge().getId().toString());
            response.setContentText(entity.getChallenge().getContentText());
            response.setPhoneticTranscriptionIpa(entity.getChallenge().getPhoneticTranscriptionIpa());
            response.setReferenceAudioUrl(entity.getChallenge().getReferenceAudioUrl());
            response.setFocusPhonemes(entity.getChallenge().getFocusPhonemes());
        }
        return response;
    }
}
