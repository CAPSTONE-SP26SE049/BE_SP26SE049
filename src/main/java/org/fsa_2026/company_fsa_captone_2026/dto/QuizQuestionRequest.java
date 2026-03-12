package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionRequest implements Serializable {

    @NotBlank(message = "Skill type is required")
    private String skillType;

    private String difficulty;

    @NotNull(message = "Question order is required")
    private Integer questionOrder;

    @NotNull(message = "Points are required")
    private Integer points;

    private java.util.UUID challengeId;
}
