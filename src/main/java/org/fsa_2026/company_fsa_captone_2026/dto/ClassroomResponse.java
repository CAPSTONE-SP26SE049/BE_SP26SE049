package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Classroom;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassroomResponse implements Serializable {

    private String id;
    private String name;
    private String code;
    private String createdBy;
    private Instant createdAt;
    private String description;
    private String dialectId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    @Schema(description = "Số lượng học sinh tối đa", example = "30")
    private Integer maxStudents;

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
                .dialectId(classroom.getDialect() != null ? classroom.getDialect().getId().toString() : null)
                .startDate(classroom.getStartDate())
                .endDate(classroom.getEndDate())
                .isActive(classroom.getIsActive())
                .maxStudents(classroom.getMaxStudents())
                .build();
    }
}
