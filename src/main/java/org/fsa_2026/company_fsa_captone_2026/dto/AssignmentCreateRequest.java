package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentCreateRequest {

    @NotNull(message = "Classroom ID is required")
    private UUID classroomId;

    @NotNull(message = "Learning unit ID is required")
    private UUID learningUnitId;

    @NotNull(message = "Due date is required")
    private Instant dueDate;

    private String status;

    private String description;
}
