package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Level;

import java.io.Serializable;

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

    public static LevelResponse fromEntity(Level level) {
        if (level == null)
            return null;
        return LevelResponse.builder()
                .id(level.getId().toString())
                .dialectId(level.getDialect() != null ? level.getDialect().getId().toString() : null)
                .levelOrder(level.getLevelOrder())
                .name(level.getName())
                .description(level.getDescription())
                .minStarsRequired(level.getMinStarsRequired())
                .errorTag(ErrorTagResponse.fromEntity(level.getErrorTag()))
                .aiThreshold(level.getAiThreshold())
                .audioUrl(level.getAudioUrl())
                .status(level.getStatus() != null ? level.getStatus().name() : null)
                .rejectionReason(level.getRejectionReason())
                .build();
    }
}
