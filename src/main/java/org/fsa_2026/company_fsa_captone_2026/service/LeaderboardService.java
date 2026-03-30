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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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

    // ─── Public API Methods ──────────────────────────────────────────────

    /**
     * Get global leaderboard with optional authenticated user's rank.
     */
    @Transactional(readOnly = true)
    public LeaderboardResponse getGlobalLeaderboard(LeaderboardPeriodType period,
                                                     LeaderboardSortBy sortBy,
                                                     String userEmail) {
        Optional<Leaderboard> lbOpt = leaderboardRepository
                .findByScopeAndPeriodTypeAndSortByAndRegionCodeIsNullAndIsFinalizedFalse(
                        LeaderboardScope.GLOBAL, period, sortBy);

        if (lbOpt.isEmpty()) {
            return buildEmptyResponse(LeaderboardScope.GLOBAL, period, sortBy, null);
        }

        Leaderboard lb = lbOpt.get();
        List<LeaderboardEntryResponse> entries = leaderboardEntryRepository
                .findTop50ByLeaderboardOrderByRankPositionAsc(lb)
                .stream()
                .map(LeaderboardEntryResponse::fromEntry)
                .collect(Collectors.toList());

        LeaderboardEntryResponse myRank = findMyRank(lb, userEmail);

        return LeaderboardResponse.builder()
                .scope(LeaderboardScope.GLOBAL.name())
                .periodType(period.name())
                .sortBy(sortBy.name())
                .regionCode(null)
                .periodStart(lb.getPeriodStart())
                .periodEnd(lb.getPeriodEnd())
                .entries(entries)
                .myRank(myRank)
                .build();
    }

    /**
     * Get regional leaderboard with optional authenticated user's rank.
     */
    @Transactional(readOnly = true)
    public LeaderboardResponse getRegionalLeaderboard(String regionCode,
                                                       LeaderboardPeriodType period,
                                                       LeaderboardSortBy sortBy,
                                                       String userEmail) {
        String normalizedRegion = regionCode.toUpperCase();
        Optional<Leaderboard> lbOpt = leaderboardRepository
                .findByScopeAndPeriodTypeAndRegionCodeAndSortByAndIsFinalizedFalse(
                        LeaderboardScope.REGIONAL, period, normalizedRegion, sortBy);

        if (lbOpt.isEmpty()) {
            return buildEmptyResponse(LeaderboardScope.REGIONAL, period, sortBy, normalizedRegion);
        }

        Leaderboard lb = lbOpt.get();
        List<LeaderboardEntryResponse> entries = leaderboardEntryRepository
                .findTop50ByLeaderboardOrderByRankPositionAsc(lb)
                .stream()
                .map(LeaderboardEntryResponse::fromEntry)
                .collect(Collectors.toList());

        LeaderboardEntryResponse myRank = findMyRank(lb, userEmail);

        return LeaderboardResponse.builder()
                .scope(LeaderboardScope.REGIONAL.name())
                .periodType(period.name())
                .sortBy(sortBy.name())
                .regionCode(normalizedRegion)
                .periodStart(lb.getPeriodStart())
                .periodEnd(lb.getPeriodEnd())
                .entries(entries)
                .myRank(myRank)
                .build();
    }

    /**
     * Get only the current user's rank for a specific leaderboard.
     */
    @Transactional(readOnly = true)
    public LeaderboardEntryResponse getMyRank(LeaderboardScope scope,
                                               String regionCode,
                                               LeaderboardPeriodType period,
                                               LeaderboardSortBy sortBy,
                                               String userEmail) {
        Optional<Leaderboard> lbOpt;
        if (scope == LeaderboardScope.GLOBAL) {
            lbOpt = leaderboardRepository
                    .findByScopeAndPeriodTypeAndSortByAndRegionCodeIsNullAndIsFinalizedFalse(
                            scope, period, sortBy);
        } else {
            String normalizedRegion = regionCode != null ? regionCode.toUpperCase() : null;
            lbOpt = leaderboardRepository
                    .findByScopeAndPeriodTypeAndRegionCodeAndSortByAndIsFinalizedFalse(
                            scope, period, normalizedRegion, sortBy);
        }

        if (lbOpt.isEmpty()) {
            return null;
        }

        return findMyRank(lbOpt.get(), userEmail);
    }

    // ─── Refresh / Snapshot Logic ────────────────────────────────────────

    /**
     * Refresh all leaderboard combinations.
     * Called by the scheduler every 15 minutes.
     */
    @Transactional
    public void refreshAllLeaderboards() {
        log.info("Starting leaderboard refresh...");
        long start = System.currentTimeMillis();

        for (LeaderboardPeriodType period : LeaderboardPeriodType.values()) {
            for (LeaderboardSortBy sortBy : LeaderboardSortBy.values()) {
                // Global leaderboard
                refreshLeaderboard(LeaderboardScope.GLOBAL, null, period, sortBy);

                // Regional leaderboards
                for (String region : new String[]{"NORTH", "CENTRAL", "SOUTH"}) {
                    refreshLeaderboard(LeaderboardScope.REGIONAL, region, period, sortBy);
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Leaderboard refresh completed in {}ms", elapsed);
    }

    /**
     * Refresh a single leaderboard snapshot.
     */
    @Transactional
    public void refreshLeaderboard(LeaderboardScope scope, String regionCode,
                                    LeaderboardPeriodType period, LeaderboardSortBy sortBy) {
        LocalDate[] periodRange = calculatePeriodRange(period);
        LocalDate periodStart = periodRange[0];
        LocalDate periodEnd = periodRange[1];

        // Find or create leaderboard record
        Leaderboard leaderboard = findOrCreateLeaderboard(scope, regionCode, period, sortBy, periodStart, periodEnd);

        // Get top 50 active accounts, already sorted by the repository
        List<Account> top50 = getAccountsForScope(scope, regionCode, sortBy);

        // Delete old entries and insert new ones
        leaderboardEntryRepository.deleteByLeaderboard(leaderboard);

        AtomicInteger rank = new AtomicInteger(1);
        List<LeaderboardEntry> entries = top50.stream()
                .map(account -> LeaderboardEntry.builder()
                        .leaderboard(leaderboard)
                        .account(account)
                        .rankPosition(rank.getAndIncrement())
                        .totalXp(account.getTotalExperience() != null ? account.getTotalExperience() : 0)
                        .totalStars(account.getTotalStars() != null ? account.getTotalStars() : 0)
                        .challengesCompleted(0) // Will be enhanced when challenge tracking is available
                        .averageScore(BigDecimal.ZERO)
                        .streakDays(account.getCurrentStreakDays() != null ? account.getCurrentStreakDays() : 0)
                        .build())
                .collect(Collectors.toList());

        leaderboardEntryRepository.saveAll(entries);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────

    private LeaderboardEntryResponse findMyRank(Leaderboard leaderboard, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return null;
        }
        Optional<Account> accountOpt = accountRepository.findByEmail(userEmail);
        if (accountOpt.isEmpty()) {
            return null;
        }
        return leaderboardEntryRepository.findByLeaderboardAndAccount(leaderboard, accountOpt.get())
                .map(LeaderboardEntryResponse::fromEntry)
                .orElse(null);
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

        Leaderboard newLb = Leaderboard.builder()
                .scope(scope)
                .periodType(period)
                .sortBy(sortBy)
                .regionCode(regionCode)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .isFinalized(false)
                .build();
        return leaderboardRepository.save(newLb);
    }

    private List<Account> getAccountsForScope(LeaderboardScope scope, String regionCode,
                                                 LeaderboardSortBy sortBy) {
        if (scope == LeaderboardScope.REGIONAL && regionCode != null) {
            return switch (sortBy) {
                case TOTAL_STARS -> accountRepository
                        .findTop50ByRegionIgnoreCaseAndIsActiveTrueOrderByTotalStarsDesc(regionCode);
                default -> accountRepository
                        .findTop50ByRegionIgnoreCaseAndIsActiveTrueOrderByTotalExperienceDesc(regionCode);
            };
        }
        return switch (sortBy) {
            case TOTAL_STARS -> accountRepository.findTop50ByIsActiveTrueOrderByTotalStarsDesc();
            default -> accountRepository.findTop50ByIsActiveTrueOrderByTotalExperienceDesc();
        };
    }

    private LocalDate[] calculatePeriodRange(LeaderboardPeriodType period) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case DAILY -> new LocalDate[]{today, today};
            case WEEKLY -> new LocalDate[]{
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            };
            case MONTHLY -> new LocalDate[]{
                    today.withDayOfMonth(1),
                    today.with(TemporalAdjusters.lastDayOfMonth())
            };
            case ALL_TIME -> new LocalDate[]{
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2099, 12, 31)
            };
        };
    }

    private LeaderboardResponse buildEmptyResponse(LeaderboardScope scope,
                                                     LeaderboardPeriodType period,
                                                     LeaderboardSortBy sortBy,
                                                     String regionCode) {
        LocalDate[] range = calculatePeriodRange(period);
        return LeaderboardResponse.builder()
                .scope(scope.name())
                .periodType(period.name())
                .sortBy(sortBy.name())
                .regionCode(regionCode)
                .periodStart(range[0])
                .periodEnd(range[1])
                .entries(List.of())
                .myRank(null)
                .build();
    }
}
