package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Request DTO for creating/updating a Quiz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizCreateRequest implements Serializable {

    @NotNull(message = "Level ID is required")
    private UUID levelId;

    @NotBlank(message = "Quiz title is required")
    private String title;

    private String description;

    private String instructions;

    @NotNull(message = "Passing score is required")
    private Integer passingScore;

    private Integer timeLimitMinutes;

    private Integer questionCount;

    private Integer pointsPerQuestion;

    private String difficulty;

    private String comment;

    private String skillType;

    @jakarta.validation.Valid
    private java.util.List<QuizQuestionRequest> questions;

    /** ID thành tựu gắn cho quiz (nullable — không bắt buộc) */
    private UUID rewardCatalogId;
}