package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotBlank;
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
public class FeedbackCreateRequest {
    @NotNull(message = "Attempt ID is required")
    private UUID attemptId;

    @NotBlank(message = "Comment is required")
    private String comment;

    @NotBlank(message = "Priority is required")
    private String priority;
}
