package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;

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

    public static LeaderboardEntryResponse fromEntity(Account account) {
        if (account == null)
            return null;
        return LeaderboardEntryResponse.builder()
                .accountId(account.getId() != null ? account.getId().toString() : null)
                .fullName(account.getFullName())
                .avatarUrl(account.getAvatarUrl())
                .totalExperience(account.getTotalExperience())
                .totalStars(account.getTotalStars())
                .currentStreakDays(account.getCurrentStreakDays())
                .region(account.getRegion())
                .build();
    }
}
