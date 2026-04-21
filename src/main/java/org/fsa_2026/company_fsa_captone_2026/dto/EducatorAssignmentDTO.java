package org.fsa_2026.company_fsa_captone_2026.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class EducatorAssignmentDTO {
    private UUID id;
    private String levelName;
    private Instant dueDate;
    private String status;
    private Instant createdAt;
    private UUID levelId;
    private UUID dialectId;
    private String metadataJson;

    // Constructor for JPQL (legacy 5-arg)
    public EducatorAssignmentDTO(UUID id, String levelName, Instant dueDate, String status, Instant createdAt) {
        this.id = id;
        this.levelName = levelName;
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Constructor for JPQL with level details (8-arg)
    @SuppressWarnings("java:S107")
    public EducatorAssignmentDTO(UUID id, String levelName, Instant dueDate, String status, Instant createdAt,
                                 UUID levelId, UUID dialectId, String metadataJson) {
        this.id = id;
        this.levelName = levelName;
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = createdAt;
        this.levelId = levelId;
        this.dialectId = dialectId;
        this.metadataJson = metadataJson;
    }
}
