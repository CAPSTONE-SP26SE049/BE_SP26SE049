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
    @JsonProperty("errorTag")
    private String errorTag;

    @JsonProperty("aiThreshold")
    private Integer aiThreshold;

    @JsonProperty("audioUrl")
    private String audioUrl;

    @JsonProperty("status")
    private String status;

    @JsonProperty("rejectionReason")
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
        String errorTag = null;
        String audioUrl = "";
        String status = "";
        String rejectionReason = "";

        try {
            String json = level.getMetadataJson();
            if (json != null && !json.isBlank()) {
                Map<String, Object> metadata = objectMapper.readValue(json, 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                
                if (level.getName().contains("D - GI - R")) {
                    System.out.println("[DEBUG] Metadata keys for D-GI-R: " + metadata.keySet());
                    System.out.println("[DEBUG] Metadata values for D-GI-R: " + metadata);
                }

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
                
                // Try multiple variants for error tag
                if (metadata.containsKey("error_tag")) {
                    errorTag = String.valueOf(metadata.get("error_tag"));
                } else if (metadata.containsKey("errorTag")) {
                    errorTag = String.valueOf(metadata.get("errorTag"));
                } else if (metadata.containsKey("error_tag_id")) {
                    errorTag = String.valueOf(metadata.get("error_tag_id"));
                }
                
                // Fallback for debugging if it's still null but we know it's D-GI-R
                if (errorTag == null && level.getName().contains("D - GI - R")) {
                    // Try to find ANY key that might be the one
                    for (String key : metadata.keySet()) {
                        if (key.toLowerCase().contains("error") || key.toLowerCase().contains("tag")) {
                            errorTag = "FOUND_IN_KEY_" + key + "_" + metadata.get(key);
                            break;
                        }
                    }
                }

                status = (String) metadata.get("status");
                rejectionReason = (String) metadata.get("rejection_reason");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to parse metadata for level " + level.getId() + ": " + e.getMessage());
        }

        return LevelResponse.builder()
                .id(level.getId().toString())
                .dialectId(level.getParent() != null ? level.getParent().getId().toString() : null)
                .levelOrder(levelOrder)
                .name(level.getName())
                .description(description)
                .minStarsRequired(minStarsRequired)
                .errorTag(errorTag)
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