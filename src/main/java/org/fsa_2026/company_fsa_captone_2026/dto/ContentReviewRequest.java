package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentReviewRequest {
    @NotNull(message = "Approval decision (status) is required. Must be APPROVED or REJECTED.")
    private ContentStatus status;

    private String rejectionReason;
    private String comment;
}
