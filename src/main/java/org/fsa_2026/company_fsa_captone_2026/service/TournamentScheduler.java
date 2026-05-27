package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Lịch trình tự động chốt giải tuần cũ và mở giải tuần mới vào mỗi thứ Hai lúc 00:00.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TournamentScheduler {

    private final TournamentService tournamentService;

    // Cron chạy vào 00:00:00 mỗi ngày thứ Hai hàng tuần
    @Scheduled(cron = "0 0 0 * * MON")
    public void rollOverWeeklyTournament() {
        log.info("Starting automatic weekly tournament rollover...");
        try {
            Map<String, Object> result = tournamentService.finalizeWeeklyTournament(null);
            log.info("Weekly tournament rollover completed successfully. Result: {}", result);
        } catch (Exception e) {
            log.error("Failed to run automatic weekly tournament rollover", e);
        }
    }
}
