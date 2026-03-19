package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizLearningUnitCreateRequest implements Serializable {

    @NotBlank(message = "Quiz name is required")
    private String name;

    @NotNull(message = "Parent level ID is required")
    private UUID parentId;

    @NotNull(message = "Pass score is required")
    private Integer passScore;

    @Valid
    @NotNull(message = "Rules are required")
    private Rules rules;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Rules implements Serializable {

        @NotNull(message = "Total questions is required")
        private Integer totalQuestions;

        @NotNull(message = "Distribution is required")
        private Map<String, Integer> distribution;

        @NotBlank(message = "Difficulty is required")
        private String difficulty;
    }
}
