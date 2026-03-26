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
import java.time.Instant;

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
    private Instant createdAt;
    private Instant updatedAt;

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
                
                // Robust parsing for level_order
                Object lo = metadata.get("level_order");
                if (lo instanceof Number) {
                    levelOrder = ((Number) lo).intValue();
                } else if (lo instanceof String) {
                    try {
                        levelOrder = Integer.parseInt((String) lo);
                    } catch (NumberFormatException ignored) {}
                }

                description = (String) metadata.get("description");

                // Robust parsing for min_stars_required
                Object msr = metadata.get("min_stars_required");
                if (msr instanceof Number) {
                    minStarsRequired = ((Number) msr).intValue();
                } else if (msr instanceof String) {
                    try {
                        minStarsRequired = Integer.parseInt((String) msr);
                    } catch (NumberFormatException ignored) {}
                }

                // Robust parsing for ai_threshold
                Object ait = metadata.get("ai_threshold");
                if (ait instanceof Number) {
                    aiThreshold = ((Number) ait).intValue();
                } else if (ait instanceof String) {
                    try {
                        aiThreshold = Integer.parseInt((String) ait);
                    } catch (NumberFormatException ignored) {}
                }

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
                .createdAt(level.getCreatedAt())
                .updatedAt(level.getUpdatedAt())
                .starsEarned(0) // Default values, populated by Service
                .isCompleted(false)
                .isLocked(true)
                .build();
    }
}