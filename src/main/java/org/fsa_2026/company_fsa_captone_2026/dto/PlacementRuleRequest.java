package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementRuleRequest {
    @NotNull(message = "Error Tag ID is required")
    private UUID errorTagId;

    @NotNull(message = "Threshold is required")
    private Integer threshold;

    @NotNull(message = "Target Dialect ID is required")
    private UUID dialectId;

    private String checkpoint;

    @NotNull(message = "Priority is required")
    private Integer priority;
}
