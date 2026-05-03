package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Login Response DTO
 * Returned after successful authentication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse implements Serializable {

    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo implements Serializable {
        private String id;
        private String email;
        private String fullName;
        private String role;
        private String region;
        @JsonProperty("avatar_url")
        private String avatarUrl;
        /** Current consecutive login streak in days */
        private int currentStreakDays;
        /** Total stars earned across all quizzes */
        private int totalStars;
        /** Total XP earned */
        private int totalExperience;
        private boolean hasDoneEntryTest;
    }
}
