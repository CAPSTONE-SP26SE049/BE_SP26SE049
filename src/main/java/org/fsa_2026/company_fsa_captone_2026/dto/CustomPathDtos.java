package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
        private Integer orderIndex;
        private String skillType;
        private Integer score;
        private Boolean isCompleted;
        private String rewardName;
        private String rewardIconUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitCustomProgressRequest {
        /** Tùy chọn — server dùng lộ trình active, không bắt buộc gửi pathId */
        private UUID pathId;

        // Fix U-06: score bắt buộc và trong khoảng 0–100 để tránh NPE/500
        @NotNull(message = "score is required")
        @Min(value = 0, message = "score must be between 0 and 100")
        @Max(value = 100, message = "score must be between 0 and 100")
        private Integer score;

        private Boolean isCompleted;
    }
}
