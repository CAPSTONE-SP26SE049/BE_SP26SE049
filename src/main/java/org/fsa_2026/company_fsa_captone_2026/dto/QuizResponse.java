package org.fsa_2026.company_fsa_captone_2026.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fsa_2026.company_fsa_captone_2026.entity.ContentItem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Integer timeLimitSeconds;
    private Integer questionCount;
    private String status;
    private String rejectionReason;
    private List<QuizQuestionResponse> questions;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static QuizResponse fromEntity(ContentItem entity) {
        if (entity == null) return null;

        String description = "";
        String instructions = "";
        Integer passingScore = 0;
        Integer timeLimitSeconds = 0;
        String rejectionReason = "";

        try {
            if (entity.getMetadataJson() != null) {
                Map<String, Object> metadata = objectMapper.readValue(entity.getMetadataJson(), Map.class);
                description = (String) metadata.get("description");
                instructions = (String) metadata.get("instructions");
                rejectionReason = (String) metadata.get("rejection_reason");

                // Robust parsing for passing_score
                Object ps = metadata.get("passing_score");
                if (ps instanceof Number) {
                    passingScore = ((Number) ps).intValue();
                } else if (ps instanceof String) {
                    try { passingScore = Integer.parseInt((String) ps); } catch (NumberFormatException ignored) {}
                }

                // Robust parsing for time_limit_seconds (fallback to minutes)
                Object tls = metadata.get("time_limit_seconds");
                Object tlm = metadata.get("time_limit_minutes");
                if (tls instanceof Number) {
                    timeLimitSeconds = ((Number) tls).intValue();
                } else if (tls instanceof String) {
                    try { timeLimitSeconds = Integer.parseInt((String) tls); } catch (NumberFormatException ignored) {}
                } else if (tlm instanceof Number) {
                    timeLimitSeconds = ((Number) tlm).intValue() * 60;
                } else if (tlm instanceof String) {
                    try { timeLimitSeconds = Integer.parseInt((String) tlm) * 60; } catch (NumberFormatException ignored) {}
                }
            }
        } catch (JsonProcessingException | ClassCastException ignored) {
            // Keep fallback values when metadata cannot be parsed.
        }

        // Map questions from itemsJson
        List<QuizQuestionResponse> questions = new ArrayList<>();
        try {
            if (entity.getItemsJson() != null) {
                List<Map<String, Object>> items = objectMapper.readValue(entity.getItemsJson(), List.class);
                questions = items.stream().map(item -> QuizQuestionResponse.builder()
                        .skillType((String) item.get("skill_type"))
                        .questionOrder((Integer) item.get("question_order"))
                        .points((Integer) item.get("points"))
                        .challengeId((String) item.get("challenge_id"))
                        .build()).collect(Collectors.toList());
            }
        } catch (JsonProcessingException | ClassCastException ignored) {
            // Keep empty question list when itemsJson cannot be parsed.
        }

        return QuizResponse.builder()
                .id(entity.getId().toString())
                .levelId(entity.getLearningUnit() != null ? entity.getLearningUnit().getId().toString() : null)
                .title(entity.getTitle())
                .description(description)
                .instructions(instructions)
                .passingScore(passingScore)
                .timeLimitSeconds(timeLimitSeconds)
                .questionCount(questions.size())
                .status(entity.getStatus())
                .rejectionReason(rejectionReason)
                .questions(questions)
                .build();
    }

    public static QuizResponse fromLearningUnit(org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit lu) {
        if (lu == null) return null;

        String description = "";
        String instructions = "";
        Integer passingScore = 0;
        Integer timeLimitSeconds = 0;
        List<QuizQuestionResponse> questions = new ArrayList<>();

        try {
            if (lu.getMetadataJson() != null) {
                Map<String, Object> metadata = objectMapper.readValue(lu.getMetadataJson(), Map.class);
                description = (String) metadata.get("description");
                instructions = (String) metadata.get("instructions");

                // Robust parsing for passing_score
                Object ps = metadata.get("passing_score");
                if (ps instanceof Number) {
                    passingScore = ((Number) ps).intValue();
                } else if (ps instanceof String) {
                    try { passingScore = Integer.parseInt((String) ps); } catch (NumberFormatException ignored) {}
                }

                // Robust parsing for time_limit_seconds (fallback to minutes)
                Object tls = metadata.get("time_limit_seconds");
                Object tlm = metadata.get("time_limit_minutes");
                if (tls instanceof Number) {
                    timeLimitSeconds = ((Number) tls).intValue();
                } else if (tls instanceof String) {
                    try { timeLimitSeconds = Integer.parseInt((String) tls); } catch (NumberFormatException ignored) {}
                } else if (tlm instanceof Number) {
                    timeLimitSeconds = ((Number) tlm).intValue() * 60;
                } else if (tlm instanceof String) {
                    try { timeLimitSeconds = Integer.parseInt((String) tlm) * 60; } catch (NumberFormatException ignored) {}
                }

                // Map questions from questions array in metadataJson
                if (metadata.get("questions") instanceof List) {
                    List<Map<String, Object>> qItems = (List<Map<String, Object>>) metadata.get("questions");
                    questions = qItems.stream().map(item -> QuizQuestionResponse.builder()
                            .skillType((String) item.get("skillType")) // Note camelCase here from buildQuizMetadata
                            .questionOrder((Integer) item.get("questionOrder"))
                            .points((Integer) item.get("points"))
                            .challengeId((String) item.get("challengeId")) // If present
                            .build()).collect(Collectors.toList());
                }
            }
        } catch (JsonProcessingException | ClassCastException ignored) {
        }

        return QuizResponse.builder()
                .id(lu.getId().toString())
                .levelId(lu.getParent() != null ? lu.getParent().getId().toString() : null)
                .title(lu.getName())
                .description(description)
                .instructions(instructions)
                .passingScore(passingScore)
                .timeLimitSeconds(timeLimitSeconds)
                .questionCount(questions.size())
                .status("APPROVED") // Default for LU quizzes
                .questions(questions)
                .build();
    }
}