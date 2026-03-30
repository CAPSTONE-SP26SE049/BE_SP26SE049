package org.fsa_2026.company_fsa_captone_2026.repository;

import java.util.Optional;
import java.util.UUID;

import org.fsa_2026.company_fsa_captone_2026.entity.Leaderboard;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardPeriodType;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardScope;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardSortBy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Leaderboard Repository
 */
@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, UUID> {

    /**
     * Find active regional leaderboard by scope, period, region, and sort criteria.
     */
    Optional<Leaderboard> findByScopeAndPeriodTypeAndRegionCodeAndSortByAndIsFinalizedFalse(
            LeaderboardScope scope,
            LeaderboardPeriodType periodType,
            String regionCode,
            LeaderboardSortBy sortBy);

    /**
     * Find active global leaderboard (regionCode is null).
     */
    Optional<Leaderboard> findByScopeAndPeriodTypeAndSortByAndRegionCodeIsNullAndIsFinalizedFalse(
            LeaderboardScope scope,
            LeaderboardPeriodType periodType,
            LeaderboardSortBy sortBy);
}
