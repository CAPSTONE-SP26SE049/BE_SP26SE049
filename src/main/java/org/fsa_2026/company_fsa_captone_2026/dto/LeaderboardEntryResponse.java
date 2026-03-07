package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.UserProfile;

import java.io.Serializable;

/**
 * Leaderboard Entry Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryResponse implements Serializable {

    private String accountId;
    private String fullName;
    private String avatarUrl;
    private Integer totalExperience;
    private Integer totalStars;
    private Integer currentStreakDays;
    private String region;

    public static LeaderboardEntryResponse fromEntity(UserProfile profile) {
        if (profile == null)
            return null;
        return LeaderboardEntryResponse.builder()
                .accountId(profile.getAccount() != null ? profile.getAccount().getId().toString() : null)
                .fullName(profile.getFullName())
                .avatarUrl(profile.getAvatarUrl())
                .totalExperience(profile.getTotalExperience())
                .totalStars(profile.getTotalStars())
                .currentStreakDays(profile.getCurrentStreakDays())
                .region(profile.getAccount() != null ? profile.getAccount().getRegion() : null)
                .build();
    }
}
