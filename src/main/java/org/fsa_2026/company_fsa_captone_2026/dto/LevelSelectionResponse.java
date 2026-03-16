package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelSelectionResponse implements Serializable {
    private String id;
    private String name;
    private String dialectId;

    public static LevelSelectionResponse fromEntity(LearningUnit level) {
        if (level == null) return null;
        return LevelSelectionResponse.builder()
                .id(level.getId().toString())
                .name(level.getName())
                .dialectId(level.getParent() != null ? level.getParent().getId().toString() : null)
                .build();
    }
}
