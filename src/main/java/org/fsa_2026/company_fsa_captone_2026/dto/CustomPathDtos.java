package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CustomPathDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCustomPathRequest {
        private String title;
        private String description;
        private List<UUID> levelIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomPathResponse {
        private UUID id;
        private String title;
        private String description;
        private List<PathLevelResponse> levels;
        private Boolean isActive;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathLevelResponse {
        private UUID levelId;
        private String levelName;
        private String region;
        private Integer orderIndex;
        private List<PathQuizResponse> quizzes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathQuizResponse {
        private UUID quizId;
        private String title;
        private Integer score;
        private Boolean isCompleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitCustomProgressRequest {
        private UUID pathId;
        private Integer score;
        private Boolean isCompleted;
    }
}
