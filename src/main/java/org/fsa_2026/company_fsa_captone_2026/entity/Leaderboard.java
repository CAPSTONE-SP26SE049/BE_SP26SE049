package org.fsa_2026.company_fsa_captone_2026.entity;

import java.time.LocalDate;

import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardPeriodType;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardScope;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardSortBy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Leaderboard Entity
 * Represents a ranked leaderboard for a specific scope, time period, and sort criteria.
 *
 * Table: leaderboard
 * FE-05: Leaderboard System with regional rankings.
 */
@Entity
@Table(name = "leaderboard")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leaderboard extends BaseEntity {

    /**
     * Scope of the leaderboard: GLOBAL or REGIONAL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 30)
    @Builder.Default
    private LeaderboardScope scope = LeaderboardScope.GLOBAL;

    /**
     * Time period type: DAILY / WEEKLY / MONTHLY / ALL_TIME
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    @Builder.Default
    private LeaderboardPeriodType periodType = LeaderboardPeriodType.WEEKLY;

    /**
     * Sort/ranking criteria: TOTAL_XP / TOTAL_STARS / CHALLENGES_COMPLETED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sort_by", nullable = false, length = 30)
    @Builder.Default
    private LeaderboardSortBy sortBy = LeaderboardSortBy.TOTAL_STARS;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /** Optional: Region code (NORTH/CENTRAL/SOUTH) for regional leaderboards */
    @Column(name = "region_code", length = 30)
    private String regionCode;

    /** Once finalized, the rankings are locked and no longer updated */
    @Column(name = "is_finalized", nullable = false)
    @Builder.Default
    private boolean isFinalized = false;
}
