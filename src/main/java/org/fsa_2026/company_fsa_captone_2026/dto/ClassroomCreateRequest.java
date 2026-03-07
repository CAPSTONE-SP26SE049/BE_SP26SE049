package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomCreateRequest implements Serializable {

    @NotBlank(message = "Tên lớp học không được để trống")
    private String name;
}
