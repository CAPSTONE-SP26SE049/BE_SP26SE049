package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.LeaderboardEntry;

import java.io.Serializable;

/**
 * Leaderboard Entry Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryResponse implements Serializable {

    private Integer rankPosition;
    private String accountId;
    private String fullName;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    private Integer totalExperience;
    private Integer totalStars;
    private Integer currentStreakDays;
    private Integer challengesCompleted;
    private String region;

    /**
     * Build from Account entity (for backwards compatibility / fallback).
     */
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

    /**
     * Build from LeaderboardEntry entity (snapshot data).
     */
    public static LeaderboardEntryResponse fromEntry(LeaderboardEntry entry) {
        if (entry == null)
            return null;
        Account account = entry.getAccount();
        return LeaderboardEntryResponse.builder()
                .rankPosition(entry.getRankPosition())
                .accountId(account != null && account.getId() != null ? account.getId().toString() : null)
                .fullName(account != null ? account.getFullName() : null)
                .avatarUrl(account != null ? account.getAvatarUrl() : null)
                .totalExperience(entry.getTotalXp())
                .totalStars(entry.getTotalStars())
                .currentStreakDays(entry.getStreakDays())
                .challengesCompleted(entry.getChallengesCompleted())
                .region(account != null ? account.getRegion() : null)
                .build();
    }
}
