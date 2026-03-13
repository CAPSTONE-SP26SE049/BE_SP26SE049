package org.fsa_2026.company_fsa_captone_2026.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomCreateRequest implements Serializable {

    @NotBlank(message = "Tên lớp học không được để trống")
    private String name;

    private String description;

    private UUID dialectId;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean isActive;

    @Schema(description = "Số lượng học sinh tối đa", example = "30")
    private Integer maxStudents;
}
