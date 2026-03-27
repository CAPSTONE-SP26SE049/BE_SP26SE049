package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorTagCreateRequest {

    @NotBlank(message = "Tag code is required")
    private String tagCode;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private List<String> regions;
}
