package org.fsa_2026.company_fsa_captone_2026.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.Leaderboard;
import org.fsa_2026.company_fsa_captone_2026.entity.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * LeaderboardEntry Repository — all queries use JOIN FETCH to avoid N+1
 */
@Repository
public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, UUID> {

    /**
     * Get top 50 entries for a leaderboard with account data in ONE query (no N+1).
     */
    @Query("SELECT e FROM LeaderboardEntry e JOIN FETCH e.account " +
           "WHERE e.leaderboard = :lb ORDER BY e.rankPosition ASC")
    List<LeaderboardEntry> findTop50WithAccountByLeaderboard(@Param("lb") Leaderboard lb);

    /**
     * Find a specific user's entry — JOIN FETCH avoids extra account query.
     */
    @Query("SELECT e FROM LeaderboardEntry e JOIN FETCH e.account a " +
           "WHERE e.leaderboard = :lb AND a = :account")
    Optional<LeaderboardEntry> findByLeaderboardAndAccount(@Param("lb") Leaderboard lb,
                                                           @Param("account") Account account);

    /**
     * Bulk delete by leaderboard (single DELETE query, not row-by-row).
     */
    @Modifying
    @Query("DELETE FROM LeaderboardEntry e WHERE e.leaderboard = :lb")
    void deleteByLeaderboard(@Param("lb") Leaderboard lb);
}
