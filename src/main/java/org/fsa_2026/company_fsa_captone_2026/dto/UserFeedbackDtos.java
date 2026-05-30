package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class UserFeedbackDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserFeedbackRequest {
        private String category;
        private String title;
        private String content;
        private String screenshotUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateFeedbackStatusRequest {
        private String status;
        private String adminNote;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserFeedbackResponse {
        private UUID id;
        private UUID senderId;
        private String senderName;
        private String senderEmail;
        private String category;
        private String title;
        private String content;
        private String status;
        private String screenshotUrl;
        private String adminNote;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedFeedbackResponse {
        private java.util.List<UserFeedbackResponse> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
