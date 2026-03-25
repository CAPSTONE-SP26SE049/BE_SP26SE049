package org.fsa_2026.company_fsa_captone_2026.dto;

import java.io.Serializable;
import java.util.Map;

import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Level Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelResponse implements Serializable {

    private String id;
    private String dialectId;
    private Integer levelOrder;
    private String name;
    private String description;
    private Integer minStarsRequired;
    private ErrorTagResponse errorTag;
    private Integer aiThreshold;
    private String audioUrl;
    private String status;
    private String rejectionReason;

    // Progression fields
    private Integer starsEarned;
    private Boolean isCompleted;
    private Boolean isLocked;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static LevelResponse fromEntity(LearningUnit level) {
        if (level == null)
            return null;

        Integer levelOrder = null;
        String description = "";
        Integer minStarsRequired = 0;
        Integer aiThreshold = null;
        String audioUrl = "";
        String status = "";
        String rejectionReason = "";

        try {
            if (level.getMetadataJson() != null) {
                Map<String, Object> metadata = objectMapper.readValue(level.getMetadataJson(), Map.class);
                levelOrder = (Integer) metadata.get("level_order");
                description = (String) metadata.get("description");
                if (metadata.get("min_stars_required") != null) {
                    minStarsRequired = (Integer) metadata.get("min_stars_required");
                }
                aiThreshold = (Integer) metadata.get("ai_threshold");
                audioUrl = (String) metadata.get("audio_url");
                status = (String) metadata.get("status");
                rejectionReason = (String) metadata.get("rejection_reason");
            }
        } catch (JsonProcessingException | ClassCastException ignored) {
            // Keep fallback values when metadata parsing fails.
        }

        return LevelResponse.builder()
                .id(level.getId().toString())
                .dialectId(level.getParent() != null ? level.getParent().getId().toString() : null)
                .levelOrder(levelOrder)
                .name(level.getName())
                .description(description)
                .minStarsRequired(minStarsRequired)
                .aiThreshold(aiThreshold)
                .audioUrl(audioUrl)
                .status(status)
                .rejectionReason(rejectionReason)
                .starsEarned(0) // Default values, populated by Service
                .isCompleted(false)
                .isLocked(true)
                .build();
    }
}