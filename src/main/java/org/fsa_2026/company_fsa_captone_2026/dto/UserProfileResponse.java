package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * User Profile Response DTO
 * GET /api/v1/users/me
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse implements Serializable {

    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String region;
    private String role;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    private Integer totalStars;
    private Integer currentStreakDays;
    private Integer totalExperience;
    private Boolean hasDoneEntryTest;
    private Instant createdAt;
}
