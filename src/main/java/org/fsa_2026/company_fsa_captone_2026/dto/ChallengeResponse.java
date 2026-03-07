package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Challenge;

import java.io.Serializable;
import java.time.Instant;

/**
 * Challenge Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeResponse implements Serializable {

    private String id;
    private String levelId;
    private String type;
    private String contentText;
    private String phoneticTranscriptionIpa;
    private String referenceAudioUrl;
    private String focusPhonemes;
    private String status;
    private String rejectionReason;
    private Instant createdAt;

    public static ChallengeResponse fromEntity(Challenge challenge) {
        if (challenge == null)
            return null;
        return ChallengeResponse.builder()
                .id(challenge.getId().toString())
                .levelId(challenge.getLevel() != null ? challenge.getLevel().getId().toString() : null)
                .type(challenge.getType())
                .contentText(challenge.getContentText())
                .phoneticTranscriptionIpa(challenge.getPhoneticTranscriptionIpa())
                .referenceAudioUrl(challenge.getReferenceAudioUrl())
                .focusPhonemes(challenge.getFocusPhonemes())
                .status(challenge.getStatus() != null ? challenge.getStatus().name() : null)
                .rejectionReason(challenge.getRejectionReason())
                .createdAt(challenge.getCreatedAt())
                .build();
    }
}
