package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomCreateRequest implements Serializable {

    @NotBlank(message = "Classroom name is required")
    private String name;

    private String description;
    private Instant startDate;
    private Instant endDate;
    private Integer currentStudents;
    private UUID dialectId;
    private Boolean isActive;
}
