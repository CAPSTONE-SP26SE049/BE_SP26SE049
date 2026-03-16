package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.ContentItem;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

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
    private String skillType;
    private String difficulty;
    private String contentText;
    private String phoneticTranscriptionIpa;
    private String referenceAudioUrl;
    private String focusPhonemes;
    private String status;
    private String rejectionReason;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ChallengeResponse fromEntity(ContentItem challenge) {
        if (challenge == null)
            return null;

        String contentText = "";
        String phoneticTranscriptionIpa = "";
        String referenceAudioUrl = "";
        String focusPhonemes = "";
        String difficulty = "";
        String rejectionReason = "";
        String skillType = "";

        try {
            if (challenge.getMetadataJson() != null) {
                Map<String, Object> metadata = objectMapper.readValue(challenge.getMetadataJson(), Map.class);
                contentText = (String) metadata.get("content_text");
                phoneticTranscriptionIpa = (String) metadata.get("phonetic_transcription_ipa");
                referenceAudioUrl = (String) metadata.get("reference_audio_url");
                focusPhonemes = (String) metadata.get("focus_phonemes");
                skillType = (String) metadata.get("skill_type");
                difficulty = (String) metadata.get("difficulty");
                rejectionReason = (String) metadata.get("rejection_reason");
            }
        } catch (Exception e) { }

        return ChallengeResponse.builder()
                .id(challenge.getId().toString())
                .levelId(challenge.getLearningUnit() != null ? challenge.getLearningUnit().getId().toString() : null)
                .type(challenge.getType())
                .skillType(skillType)
                .difficulty(difficulty)
                .contentText(contentText)
                .phoneticTranscriptionIpa(phoneticTranscriptionIpa)
                .referenceAudioUrl(referenceAudioUrl)
                .focusPhonemes(focusPhonemes)
                .status(challenge.getStatus())
                .rejectionReason(rejectionReason)
                .build();
    }
}