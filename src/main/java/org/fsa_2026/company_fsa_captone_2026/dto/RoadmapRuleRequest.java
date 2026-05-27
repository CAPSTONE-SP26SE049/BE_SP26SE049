package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapRuleRequest {
    private UUID id;

    @NotNull(message = "minPercent không được null")
    @Min(value = 0, message = "minPercent phải >= 0")
    @Max(value = 100, message = "minPercent phải <= 100")
    private Double minPercent;

    @NotNull(message = "maxPercent không được null")
    @Min(value = 0, message = "maxPercent phải >= 0")
    @Max(value = 100, message = "maxPercent phải <= 100")
    private Double maxPercent;

    @NotBlank(message = "difficulties không được trống")
    private String difficulties;

    @Builder.Default
    private Boolean isActive = true;

    @AssertTrue(message = "minPercent phải nhỏ hơn hoặc bằng maxPercent")
    public boolean isValidRange() {
        if (minPercent == null || maxPercent == null) return true;
        return minPercent <= maxPercent;
    }
}
