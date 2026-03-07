package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelCreateRequest {

    @NotNull(message = "Dialect ID is required")
    private UUID dialectId;

    @NotNull(message = "Level order is required")
    @Min(value = 1, message = "Level order must be at least 1")
    private Integer levelOrder;

    @NotBlank(message = "Level name is required")
    private String name;

    private java.util.UUID errorTagId;

    private String description;

    @Min(value = 0, message = "Minimum stars required must be 0 or greater")
    private Integer minStarsRequired;

    private String comment;
}
