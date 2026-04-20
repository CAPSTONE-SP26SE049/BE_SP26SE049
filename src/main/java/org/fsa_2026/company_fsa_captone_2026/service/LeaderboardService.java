package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.LeaderboardEntryResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LeaderboardResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.Leaderboard;
import org.fsa_2026.company_fsa_captone_2026.entity.LeaderboardEntry;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardPeriodType;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardScope;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardSortBy;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LeaderboardEntryRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LeaderboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final AccountRepository accountRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final LeaderboardEntryRepository leaderboardEntryRepository;

    // ─── Canonical regions (normalized) ──────────────────────────────────────
    private static final String[] ACTIVE_REGIONS = {"NORTH", "CENTRAL", "SOUTH"};

    // ─── Public API Methods ──────────────────────────────────────────────

    /**
     * Get global leaderboard — reads from snapshot (fast).
     * Only refreshes lazily if snapshot missing.
     */
    @Transactional
    public LeaderboardResponse getGlobalLeaderboard(LeaderboardPeriodType period,
                                                     LeaderboardSortBy sortBy,
                                                     String userEmail) {
        // We now only support ALL_TIME + TOTAL_STARS. Normalize.
        final LeaderboardPeriodType p = LeaderboardPeriodType.ALL_TIME;
        final LeaderboardSortBy s    = LeaderboardSortBy.TOTAL_STARS;

        // Check snapshot existence
        Optional<Leaderboard> lbOpt = leaderboardRepository
                .findByScopeAndPeriodTypeAndSortByAndRegionCodeIsNullAndIsFinalizedFalse(
                        LeaderboardScope.GLOBAL, p, s);

        if (lbOpt.isEmpty()) {
            log.info("[Leaderboard] No global snapshot found, running one-time refresh...");
            refreshLeaderboard(LeaderboardScope.GLOBAL, null, p, s);
            lbOpt = leaderboardRepository
                    .findByScopeAndPeriodTypeAndSortByAndRegionCodeIsNullAndIsFinalizedFalse(
                            LeaderboardScope.GLOBAL, p, s);
        }

        if (lbOpt.isEmpty()) return buildEmptyResponse(LeaderboardScope.GLOBAL, p, s, null);

        return buildResponse(lbOpt.get(), LeaderboardScope.GLOBAL, p, s, null, userEmail);
    }

    /**
     * Get regional leaderboard for a canonical region code (NORTH/CENTRAL/SOUTH).
     */
    @Transactional
    public LeaderboardResponse getRegionalLeaderboard(String regionCode,
                                                       LeaderboardPeriodType period,
                                                       LeaderboardSortBy sortBy,
                                                       String userEmail) {
        final LeaderboardPeriodType p = LeaderboardPeriodType.ALL_TIME;
        final LeaderboardSortBy s    = LeaderboardSortBy.TOTAL_STARS;
        String normalizedRegion = normalizeRegion(regionCode);

        Optional<Leaderboard> lbOpt = leaderboardRepository
                .findByScopeAndPeriodTypeAndRegionCodeAndSortByAndIsFinalizedFalse(
                        LeaderboardScope.REGIONAL, p, normalizedRegion, s);

        if (lbOpt.isEmpty()) {
            log.info("[Leaderboard] No regional snapshot for {}, running one-time refresh...", normalizedRegion);
            refreshLeaderboard(LeaderboardScope.REGIONAL, normalizedRegion, p, s);
            lbOpt = leaderboardRepository
                    .findByScopeAndPeriodTypeAndRegionCodeAndSortByAndIsFinalizedFalse(
                            LeaderboardScope.REGIONAL, p, normalizedRegion, s);
        }

        if (lbOpt.isEmpty()) return buildEmptyResponse(LeaderboardScope.REGIONAL, p, s, normalizedRegion);

        return buildResponse(lbOpt.get(), LeaderboardScope.REGIONAL, p, s, normalizedRegion, userEmail);
    }

    /**
     * Get only the current user's rank.
     */
    @Transactional(readOnly = true)
    public LeaderboardEntryResponse getMyRank(LeaderboardScope scope,
                                               String regionCode,
                                               LeaderboardPeriodType period,
                                               LeaderboardSortBy sortBy,
                                               String userEmail) {
        period = LeaderboardPeriodType.ALL_TIME;
        sortBy = LeaderboardSortBy.TOTAL_STARS;

        Optional<Leaderboard> lbOpt;
        if (scope == LeaderboardScope.GLOBAL) {
            lbOpt = leaderboardRepository
                    .findByScopeAndPeriodTypeAndSortByAndRegionCodeIsNullAndIsFinalizedFalse(scope, period, sortBy);
        } else {
            String nr = normalizeRegion(regionCode);
            lbOpt = leaderboardRepository
                    .findByScopeAndPeriodTypeAndRegionCodeAndSortByAndIsFinalizedFalse(scope, period, nr, sortBy);
        }

        return lbOpt.map(lb -> findMyRank(lb, userEmail)).orElse(null);
    }

    // ─── Scheduled Refresh (called by Scheduler every 5 min) ─────────────────

    /**
     * Optimized refresh: only 4 leaderboards (GLOBAL + 3 regions), all ALL_TIME + TOTAL_STARS.
     * This replaced the old 108-combination refresh loop.
     */
    @Transactional
    public void refreshAllLeaderboards() {
        log.info("[Leaderboard] Starting optimized refresh (4 boards)...");
        long start = System.currentTimeMillis();

        final LeaderboardPeriodType period = LeaderboardPeriodType.ALL_TIME;
        final LeaderboardSortBy sortBy     = LeaderboardSortBy.TOTAL_STARS;

        // 1. Global
        refreshLeaderboard(LeaderboardScope.GLOBAL, null, period, sortBy);

        // 2. Three canonical regions
        for (String region : ACTIVE_REGIONS) {
            refreshLeaderboard(LeaderboardScope.REGIONAL, region, period, sortBy);
        }

        log.info("[Leaderboard] Refresh completed in {}ms", System.currentTimeMillis() - start);
    }

    /**
     * Refresh a single leaderboard snapshot.
     */
    @Transactional
    public void refreshLeaderboard(LeaderboardScope scope, String regionCode,
                                    LeaderboardPeriodType period, LeaderboardSortBy sortBy) {
        // Only support TOTAL_STARS for now
        sortBy = LeaderboardSortBy.TOTAL_STARS;

        LocalDate[] range = calculatePeriodRange(period);

        // Find or create leaderboard header row
        Leaderboard leaderboard = findOrCreateLeaderboard(scope, regionCode, period, sortBy, range[0], range[1]);

        // Fetch top 50 accounts ordered by totalStars DESC
        List<Account> top50 = getTop50AccountsForScope(scope, regionCode);

        // Bulk delete stale entries (single DELETE query), then insert fresh
        leaderboardEntryRepository.deleteByLeaderboard(leaderboard);

        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardEntry> entries = top50.stream()
                .map(account -> LeaderboardEntry.builder()
                        .leaderboard(leaderboard)
                        .account(account)
                        .rankPosition(rank.getAndIncrement())
                        .totalXp(0) // XP removed from system
                        .totalStars(account.getTotalStars() != null ? account.getTotalStars() : 0)
                        .challengesCompleted(0) // Account entity does not track this directly
                        .averageScore(BigDecimal.ZERO)
                        .streakDays(account.getCurrentStreakDays() != null ? account.getCurrentStreakDays() : 0)
                        .build())
                .collect(Collectors.toList());

        leaderboardEntryRepository.saveAll(entries);
        log.debug("[Leaderboard] Refreshed {} entries for scope={} region={}", entries.size(), scope, regionCode);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    /**
     * Normalize any region alias to the canonical form: NORTH / CENTRAL / SOUTH.
     */
    private String normalizeRegion(String raw) {
        if (raw == null) return "SOUTH";
        return switch (raw.toUpperCase().trim()) {
            case "NORTH", "MIEN_BAC", "BAC" -> "NORTH";
            case "CENTRAL", "MIEN_TRUNG", "TRUNG" -> "CENTRAL";
            default -> "SOUTH";
        };
    }

    /**
     * Returns top 50 active accounts sorted by totalStars DESC.
     */
    private List<Account> getTop50AccountsForScope(LeaderboardScope scope, String regionCode) {
        if (scope == LeaderboardScope.REGIONAL && regionCode != null) {
            return accountRepository.findTop50ByRegionIgnoreCaseAndIsActiveTrueOrderByTotalStarsDesc(regionCode);
        }
        return accountRepository.findTop50ByIsActiveTrueOrderByTotalStarsDesc();
    }

    private LeaderboardEntryResponse findMyRank(Leaderboard leaderboard, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) return null;
        return accountRepository.findByEmail(userEmail)
                .flatMap(account -> leaderboardEntryRepository.findByLeaderboardAndAccount(leaderboard, account))
                .map(LeaderboardEntryResponse::fromEntry)
                .orElse(null);
    }

    /**
     * Build LeaderboardResponse from existing snapshot — uses JOIN FETCH (1 query for all entries).
     */
    @Transactional(readOnly = true)
    private LeaderboardResponse buildResponse(Leaderboard lb, LeaderboardScope scope,
                                               LeaderboardPeriodType period, LeaderboardSortBy sortBy,
                                               String regionCode, String userEmail) {
        List<LeaderboardEntryResponse> entries = leaderboardEntryRepository
                .findTop50WithAccountByLeaderboard(lb)   // JOIN FETCH — no N+1
                .stream()
                .map(LeaderboardEntryResponse::fromEntry)
                .collect(Collectors.toList());

        LeaderboardEntryResponse myRank = findMyRank(lb, userEmail);

        return LeaderboardResponse.builder()
                .scope(scope.name())
                .periodType(period.name())
                .sortBy(sortBy.name())
                .regionCode(regionCode)
                .periodStart(lb.getPeriodStart())
                .periodEnd(lb.getPeriodEnd())
                .entries(entries)
                .myRank(myRank)
                .build();
    }

    private Leaderboard findOrCreateLeaderboard(LeaderboardScope scope, String regionCode,
                                                  LeaderboardPeriodType period, LeaderboardSortBy sortBy,
                                                  LocalDate periodStart, LocalDate periodEnd) {
        Optional<Leaderboard> existing;
        if (scope == LeaderboardScope.GLOBAL) {
            existing = leaderboardRepository
                    .findByScopeAndPeriodTypeAndSortByAndRegionCodeIsNullAndIsFinalizedFalse(scope, period, sortBy);
        } else {
            existing = leaderboardRepository
                    .findByScopeAndPeriodTypeAndRegionCodeAndSortByAndIsFinalizedFalse(scope, period, regionCode, sortBy);
        }

        if (existing.isPresent()) {
            Leaderboard lb = existing.get();
            lb.setPeriodStart(periodStart);
            lb.setPeriodEnd(periodEnd);
            return leaderboardRepository.save(lb);
        }

        return leaderboardRepository.save(Leaderboard.builder()
                .scope(scope)
                .periodType(period)
                .sortBy(sortBy)
                .regionCode(regionCode)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .isFinalized(false)
                .build());
    }

    private LocalDate[] calculatePeriodRange(LeaderboardPeriodType period) {
        // We only use ALL_TIME now; keep method for compatibility
        return new LocalDate[]{ LocalDate.of(2020, 1, 1), LocalDate.of(2099, 12, 31) };
    }

    private LeaderboardResponse buildEmptyResponse(LeaderboardScope scope,
                                                     LeaderboardPeriodType period,
                                                     LeaderboardSortBy sortBy,
                                                     String regionCode) {
        return LeaderboardResponse.builder()
                .scope(scope.name())
                .periodType(period.name())
                .sortBy(sortBy.name())
                .regionCode(regionCode)
                .periodStart(LocalDate.of(2020, 1, 1))
                .periodEnd(LocalDate.of(2099, 12, 31))
                .entries(List.of())
                .myRank(null)
                .build();
    }
}
