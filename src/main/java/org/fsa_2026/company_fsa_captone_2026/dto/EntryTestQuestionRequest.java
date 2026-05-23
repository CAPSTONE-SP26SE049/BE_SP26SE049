package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.EntryTestRegionCategory;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryTestQuestionRequest {
    @NotBlank(message = "Target text is required")
    private String targetText;

    @NotNull(message = "Region category is required")
    private EntryTestRegionCategory regionCategory;
}
