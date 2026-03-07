package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.ErrorTag;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorTagResponse implements Serializable {

    private String id;
    private String tagCode;
    private String name;
    private String description;
    private java.util.List<String> regions;

    public static ErrorTagResponse fromEntity(ErrorTag tag) {
        if (tag == null)
            return null;
        return ErrorTagResponse.builder()
                .id(tag.getId().toString())
                .tagCode(tag.getTagCode())
                .name(tag.getName())
                .description(tag.getDescription())
                .regions(tag.getPlacementRules() != null ? tag.getPlacementRules().stream()
                        .map(rule -> rule.getTargetDialect().getName())
                        .distinct()
                        .toList() : null)
                .build();
    }
}
