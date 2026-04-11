package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that refreshes all leaderboard snapshots periodically.
 * Runs every 15 minutes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardScheduler {

    private final LeaderboardService leaderboardService;

    // @Scheduled(fixedRate = 300000, initialDelay = 10000) // DISABLED: Causing 17s DB lock and HikariCP connection leak
    public void refreshLeaderboards() {
        try {
            leaderboardService.refreshAllLeaderboards();
        } catch (Exception e) {
            log.error("Failed to refresh leaderboards", e);
        }
    }
}
