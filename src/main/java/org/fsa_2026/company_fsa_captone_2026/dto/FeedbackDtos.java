package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class FeedbackDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFeedbackRequest {
        private UUID studentId;
        private UUID attemptId; // SpeakingAttempt ID
        private String comment;
        private String priority; // e.g., LOW, MEDIUM, HIGH
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackResponse {
        private UUID id;
        private UUID educatorId;
        private String educatorName;
        private String studentName;
        private String comment;
        private String priority;
        private Instant createdAt;
        private UUID attemptId;
        private String targetText;
        private String audioUrl;
        private Integer groqScore;
        private String groqFeedback;
        private String asrTranscription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpeakingAttemptResponse {
        private UUID id;
        private String targetText;
        private String asrTranscription;
        private String audioUrl;
        private Integer groqScore;
        private String groqFeedback;
        private Instant createdAt;
    }
}
