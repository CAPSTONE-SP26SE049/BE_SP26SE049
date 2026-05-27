package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class EducatorJourneyDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentLearningPathResponse {
        private UUID id;
        private String title;
        private String description;
        private String focusArea;
        private List<String> milestones;
        private String status;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAccountResponse {
        private UUID id;
        private String fullName;
        private String email;
        @JsonProperty("avatar_url")
        private String avatarUrl;
        private String level;
        private StudentLearningPathResponse learningPath;
        private LocalDateTime lastActiveAt;
        private Integer progressPercent;
        private Integer pronunciationScore;
        private List<String> weakPhonemes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PronunciationMetricResponse {
        private String label;
        private Integer value;
        private String trend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressOverviewResponse {
        private Long totalStudents;
        private Long activeStudents;
        private Double averagePronunciationScore;
        private Long pendingFeedbackCount;
        private Double weeklyProgressRate;
        private List<PronunciationMetricResponse> pronunciationMetrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonPlanResponse {
        private UUID id;
        private String title;
        private String objective;
        private List<String> targetStudents;
        private List<String> achievementGoals;
        private String status;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackMessageResponse {
        private UUID id;
        private UUID studentId;
        private String studentName;
        private String content;
        private String channel;
        private String priority;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsReportResponse {
        private UUID studentId;
        private String studentName;
        private List<PronunciationErrorResponse> pronunciationErrors;
        private List<EffectivenessPointResponse> learningEffectiveness;
        private List<SessionPointResponse> recentSessions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PronunciationErrorResponse {
        private String phoneme;
        private Integer count;
        private Double accuracy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EffectivenessPointResponse {
        private String label;
        private Integer value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionPointResponse {
        private UUID id;
        private Integer score;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomLearningPathRequest {
        @jakarta.validation.constraints.NotNull(message = "studentId không được null")
        private UUID studentId;

        @jakarta.validation.constraints.NotBlank(message = "title không được trống")
        private String title;

        @jakarta.validation.constraints.NotBlank(message = "focusArea không được trống")
        private String focusArea;

        private List<String> milestones;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonPlanRequest {
        @jakarta.validation.constraints.NotBlank(message = "title không được trống")
        private String title;

        @jakarta.validation.constraints.NotBlank(message = "objective không được trống")
        private String objective;

        private List<String> targetStudents;
        private List<String> achievementGoals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageRequest {
        private UUID studentId;
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackRequest {
        private UUID studentId;
        private String content;
        private String priority;
    }
}
