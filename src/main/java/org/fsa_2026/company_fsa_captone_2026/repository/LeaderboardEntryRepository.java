package org.fsa_2026.company_fsa_captone_2026.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.Leaderboard;
import org.fsa_2026.company_fsa_captone_2026.entity.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * LeaderboardEntry Repository
 */
@Repository
public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, UUID> {

    /**
     * Get top N entries for a leaderboard, ordered by rank.
     */
    List<LeaderboardEntry> findTop50ByLeaderboardOrderByRankPositionAsc(Leaderboard leaderboard);

    /**
     * Find a specific user's entry in a leaderboard.
     */
    Optional<LeaderboardEntry> findByLeaderboardAndAccount(Leaderboard leaderboard, Account account);

    /**
     * Delete all entries for a leaderboard (used during refresh).
     */
    void deleteByLeaderboard(Leaderboard leaderboard);
}
