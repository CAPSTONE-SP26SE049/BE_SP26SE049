package org.fsa_2026.company_fsa_captone_2026.dto;

<<<<<<< HEAD
=======
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
>>>>>>> e99e602 (Remove classroom domain and drop schema)
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

    // Constructor for JPQL (old 6-arg for backward compat)
    public EducatorAssignmentDTO(UUID id, String classroomName, String levelName, Instant dueDate, String status, Instant createdAt) {
        this.id = id;
        this.classroomName = classroomName;
        this.levelName = levelName;
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Constructor for JPQL with level details (9-arg)
    public EducatorAssignmentDTO(UUID id, String classroomName, String levelName, Instant dueDate, String status, Instant createdAt, UUID levelId, UUID dialectId, String metadataJson) {
        this.id = id;
        this.classroomName = classroomName;
        this.levelName = levelName;
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = createdAt;
        this.levelId = levelId;
        this.dialectId = dialectId;
        this.metadataJson = metadataJson;
    }
}
