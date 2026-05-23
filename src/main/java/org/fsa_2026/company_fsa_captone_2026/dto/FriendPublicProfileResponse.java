package org.fsa_2026.company_fsa_captone_2026.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Public profile DTO — exposed to friends only.
 * Excludes all sensitive data: email, phone, password hash,
 * verification codes, reset codes, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendPublicProfileResponse {

    private UUID id;

    /** Display name */
    private String fullName;

    /** Avatar URL */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** Region (NORTH / CENTRAL / SOUTH) — not sensitive */
    private String region;

    /** Gamification stats — public */
    private Integer totalStars;
    private Integer currentStreakDays;
    private Integer totalExperience;

    /** Member since */
    private Instant memberSince;
}
