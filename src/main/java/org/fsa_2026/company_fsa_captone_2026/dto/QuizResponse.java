package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.ContentItem;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static QuizResponse fromEntity(ContentItem entity) {
        if (entity == null) return null;

        String description = "";
        String instructions = "";
        Integer passingScore = 0;
        Integer timeLimitMinutes = 0;
        String rejectionReason = "";

        try {
            if (entity.getMetadataJson() != null) {
                Map<String, Object> metadata = objectMapper.readValue(entity.getMetadataJson(), Map.class);
                description = (String) metadata.get("description");
                instructions = (String) metadata.get("instructions");
                passingScore = (Integer) metadata.get("passing_score");
                timeLimitMinutes = (Integer) metadata.get("time_limit_minutes");
                rejectionReason = (String) metadata.get("rejection_reason");
            }
        } catch (Exception e) { }

        // Map questions from itemsJson
        List<QuizQuestionResponse> questions = null;
        try {
            if (entity.getItemsJson() != null) {
                List<Map<String, Object>> items = objectMapper.readValue(entity.getItemsJson(), List.class);
                questions = items.stream().map(item -> {
                    return QuizQuestionResponse.builder()
                        .skillType((String) item.get("skill_type"))
                        .difficulty((String) item.get("difficulty"))
                        .questionOrder((Integer) item.get("question_order"))
                        .points((Integer) item.get("points"))
                        .challengeId((String) item.get("challenge_id"))
                        .build();
                }).collect(Collectors.toList());
            }
        } catch (Exception e) { }

        return QuizResponse.builder()
                .id(entity.getId().toString())
                .levelId(entity.getLearningUnit() != null ? entity.getLearningUnit().getId().toString() : null)
                .title(entity.getTitle())
                .description(description)
                .instructions(instructions)
                .passingScore(passingScore)
                .timeLimitMinutes(timeLimitMinutes)
                .status(entity.getStatus())
                .rejectionReason(rejectionReason)
                .questions(questions)
                .build();
    }
}