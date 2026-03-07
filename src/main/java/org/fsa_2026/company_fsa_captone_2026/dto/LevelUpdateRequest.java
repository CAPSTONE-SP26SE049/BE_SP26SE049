package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelUpdateRequest {
    private String name;
    private java.util.UUID errorTagId;
    private Integer aiThreshold;
    private String comment;
}
