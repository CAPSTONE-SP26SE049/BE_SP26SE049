package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Leaderboard Response DTO
 * Wraps leaderboard metadata together with ranked entries and current user's rank.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardResponse implements Serializable {

    /** Scope: GLOBAL or REGIONAL */
    private String scope;

    /** Period type: DAILY / WEEKLY / MONTHLY / ALL_TIME */
    private String periodType;

    /** Sort criteria: TOTAL_XP / TOTAL_STARS / CHALLENGES_COMPLETED */
    private String sortBy;

    /** Region code (null for global leaderboards) */
    private String regionCode;

    /** Period start date */
    private LocalDate periodStart;

    /** Period end date */
    private LocalDate periodEnd;

    /** Top 50 ranked entries */
    private List<LeaderboardEntryResponse> entries;

    /** Current authenticated user's rank (null if not authenticated or not ranked) */
    private LeaderboardEntryResponse myRank;
}
