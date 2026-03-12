package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.PlacementRule;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementRuleResponse implements Serializable {
    private String id;
    private ErrorTagResponse errorTag;
    private Integer threshold;
    private String dialectId;
    private String checkpoint;
    private Integer priority;

    public static PlacementRuleResponse fromEntity(PlacementRule entity) {
        if (entity == null) return null;
        return PlacementRuleResponse.builder()
                .id(entity.getId().toString())
                .errorTag(ErrorTagResponse.fromEntity(entity.getErrorTag()))
                .threshold(entity.getThreshold())
                .dialectId(entity.getTargetDialect() != null ? entity.getTargetDialect().getId().toString() : null)
                .checkpoint(entity.getCheckpoint())
                .priority(entity.getPriority())
                .build();
    }
}
