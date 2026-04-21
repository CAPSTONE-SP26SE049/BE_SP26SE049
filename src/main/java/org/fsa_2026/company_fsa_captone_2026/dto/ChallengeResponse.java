package org.fsa_2026.company_fsa_captone_2026.dto;

import java.io.Serializable;
import java.util.Map;

import org.fsa_2026.company_fsa_captone_2026.entity.ContentItem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String contentText;
    private String phoneticTranscriptionIpa;
    private String referenceAudioUrl;
    private String focusPhonemes;
    private String status;
    private String rejectionReason;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static ChallengeResponse fromEntity(ContentItem challenge) {
        if (challenge == null)
            return null;

        String contentText = "";
        String phoneticTranscriptionIpa = "";
        String referenceAudioUrl = "";
        String focusPhonemes = "";
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
                rejectionReason = (String) metadata.get("rejection_reason");
            }
        } catch (JsonProcessingException | ClassCastException ignored) {
            // Keep default response values when metadata parsing fails.
        }

        return ChallengeResponse.builder()
                .id(challenge.getId().toString())
                .levelId(challenge.getLearningUnit() != null ? challenge.getLearningUnit().getId().toString() : null)
                .type(challenge.getType())
                .skillType(skillType)
                .contentText(contentText)
                .phoneticTranscriptionIpa(phoneticTranscriptionIpa)
                .referenceAudioUrl(referenceAudioUrl)
                .focusPhonemes(focusPhonemes)
                .status(challenge.getStatus())
                .rejectionReason(rejectionReason)
                .build();
    }
}