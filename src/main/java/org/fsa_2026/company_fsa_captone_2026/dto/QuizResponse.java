package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Quiz;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for Quiz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponse implements Serializable {
    private String id;
    private String levelId;
    private String title;
    private String description;
    private String instructions;
    private Integer passingScore;
    private Integer timeLimitMinutes;
    private Integer questionCount;
    private String status;
    private String rejectionReason;
    private List<QuizQuestionResponse> questions;

    public static QuizResponse fromEntity(Quiz entity) {
        if (entity == null) return null;
        return QuizResponse.builder()
                .id(entity.getId().toString())
                .levelId(entity.getLevel() != null ? entity.getLevel().getId().toString() : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .instructions(entity.getInstructions())
                .passingScore(entity.getPassingScore())
                .timeLimitMinutes(entity.getTimeLimitMinutes())
                .questionCount(entity.getQuestionCount())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .rejectionReason(entity.getRejectionReason())
                .questions(entity.getQuestions() != null ? 
                        entity.getQuestions().stream()
                                .map(QuizQuestionResponse::fromEntity)
                                .collect(Collectors.toList()) : null)
                .build();
    }
}