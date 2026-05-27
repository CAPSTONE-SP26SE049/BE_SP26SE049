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

        @jakarta.validation.constraints.NotBlank(message = "Comment không được trống")
        private String comment;

        private String priority; // e.g., LOW, MEDIUM, HIGH

        @jakarta.validation.constraints.AssertTrue(message = "Phải cung cấp ít nhất studentId hoặc attemptId")
        public boolean isValidRequest() {
            return studentId != null || attemptId != null;
        }
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
        private Integer asrScore;
        private String wordDetails;
        private String recordId;
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
        private Integer asrScore;
        private String wordDetails;
        private String recordId;
    }
}
