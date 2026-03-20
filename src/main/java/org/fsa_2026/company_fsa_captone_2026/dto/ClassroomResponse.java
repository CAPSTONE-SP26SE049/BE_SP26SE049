package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Classroom;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomResponse implements Serializable {

    private String id;
    private String name;
    private String code;
    private String createdBy;
    private Instant createdAt;
    private String description;
    private Instant startDate;
    private Instant endDate;
    private Integer currentStudents;
    private String dialectId;
    private Boolean isActive;

    public static ClassroomResponse fromEntity(Classroom classroom) {
        if (classroom == null)
            return null;
        return ClassroomResponse.builder()
                .id(classroom.getId().toString())
                .name(classroom.getName())
                .code(classroom.getCode())
                .createdBy(classroom.getEducator() != null ? classroom.getEducator().getId().toString() : null)
                .createdAt(classroom.getCreatedAt())
                .description(classroom.getDescription())
                .startDate(classroom.getStartDate())
                .endDate(classroom.getEndDate())
                .currentStudents(classroom.getCurrentStudents())
                .dialectId(classroom.getDialect() != null ? classroom.getDialect().getId().toString() : null)
                .isActive(classroom.getIsActive())
                .build();
    }
}
