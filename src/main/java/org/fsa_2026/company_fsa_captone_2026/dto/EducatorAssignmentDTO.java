package org.fsa_2026.company_fsa_captone_2026.dto;

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
public class EducatorAssignmentDTO {
    private UUID id;
    private String classroomName;
    private String levelName;
    private Instant dueDate;
    private String status;
    private Instant createdAt;
}
