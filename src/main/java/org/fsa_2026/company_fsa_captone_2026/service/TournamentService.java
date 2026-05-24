package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountReward;
import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;
import org.fsa_2026.company_fsa_captone_2026.entity.Tournament;
import org.fsa_2026.company_fsa_captone_2026.entity.TournamentParticipant;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRewardRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.RewardCatalogRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.TournamentParticipantRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;
    private final AccountRepository accountRepository;
    private final RewardCatalogRepository rewardCatalogRepository;
    private final AccountRewardRepository accountRewardRepository;
    private final BadgeUnlockService badgeUnlockService;

    /**
     * Chốt giải đấu tuần hiện tại, trao giải thưởng XP và huy hiệu cho Top 3, 
     * đồng thời tạo và kích hoạt giải đấu cho tuần kế tiếp.
     */
    @Transactional
    public Map<String, Object> finalizeWeeklyTournament(UUID tournamentId) {
        Tournament tournament = null;
        if (tournamentId != null) {
            tournament = tournamentRepository.findById(tournamentId)
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy giải đấu"));
        } else {
            // Lấy giải đấu tuần ACTIVE đầu tiên
            List<Tournament> activeWeeklies = tournamentRepository.findByTypeAndStatus("WEEKLY", "ACTIVE");
            if (!activeWeeklies.isEmpty()) {
                tournament = activeWeeklies.get(0);
            }
        }

        if (tournament == null) {
            log.info("No active weekly tournament found to finalize. Finalizing based on Global Stars Leaderboard.");
        } else {
            if ("FINISHED".equalsIgnoreCase(tournament.getStatus())) {
                throw new ApiException("BAD_REQUEST", "Giải đấu này đã được chốt từ trước.");
            }
            tournament.setStatus("FINISHED");
            tournamentRepository.save(tournament);
            log.info("Weekly tournament '{}' is marked as FINISHED.", tournament.getName());
        }

        // Lấy Top 3 người chiến thắng
        List<Account> winners = new ArrayList<>();
        if (tournament != null) {
            List<TournamentParticipant> participants = tournamentParticipantRepository
                    .findByTournamentIdOrderByTotalXpDesc(tournament.getId());
            for (TournamentParticipant p : participants) {
                if (winners.size() < 3) {
                    winners.add(p.getAccount());
                }
            }
        }

        // Fallback: nếu không có giải đấu hoặc giải đấu không có người tham gia, dùng Global Stars Leaderboard
        if (winners.isEmpty()) {
            log.info("Fallback to Global Stars Leaderboard for top 3 users.");
            List<Account> activeUsers = accountRepository
                    .findTop50ByIsActiveTrueOrderByTotalStarsDescBadgeCountDescCurrentStreakDaysDesc();
            for (Account user : activeUsers) {
                if (winners.size() < 3) {
                    winners.add(user);
                }
            }
        }

        // Trao giải thưởng cho Top 3
        int[] xpPrizes = {500, 250, 100};
        String[] titles = {"Quán Quân", "Á Quân", "Hạng Ba"};
        
        for (int i = 0; i < winners.size(); i++) {
            Account user = winners.get(i);
            int xpReward = xpPrizes[i];
            user.setTotalExperience((user.getTotalExperience() != null ? user.getTotalExperience() : 0) + xpReward);
            accountRepository.save(user);
            log.info("Awarded {} XP to {} ({}) as {}", xpReward, user.getFullName(), user.getEmail(), titles[i]);
            
            // Quét badge sau khi cộng XP
            badgeUnlockService.checkAndUnlockBadges(user);
        }

        // Trao cúp vô địch (Badge TOURNAMENT_WINNER) cho Quán Quân (Top 1)
        if (!winners.isEmpty()) {
            Account champion = winners.get(0);
            Optional<RewardCatalog> winnerBadgeOpt = rewardCatalogRepository.findByCode("TOURNAMENT_WINNER");
            if (winnerBadgeOpt.isPresent()) {
                RewardCatalog winnerBadge = winnerBadgeOpt.get();
                Optional<AccountReward> existingCup = accountRewardRepository
                        .findByAccountIdAndRewardCatalogId(champion.getId(), winnerBadge.getId());
                if (existingCup.isEmpty()) {
                    AccountReward cupReward = AccountReward.builder()
                            .account(champion)
                            .rewardCatalog(winnerBadge)
                            .status("UNLOCKED")
                            .progressValue(100)
                            .unlockedAt(Instant.now())
                            .build();
                    accountRewardRepository.save(cupReward);
                    
                    champion.setBadgeCount((champion.getBadgeCount() != null ? champion.getBadgeCount() : 0) + 1);
                    accountRepository.save(champion);
                    log.info("Awarded TOURNAMENT_WINNER badge to Champion: {}", champion.getEmail());
                }
            }
        }

        // Trao huy chương tham chiến (Badge TOURNAMENT_PARTICIPANT) cho Top 3
        Optional<RewardCatalog> partBadgeOpt = rewardCatalogRepository.findByCode("TOURNAMENT_PARTICIPANT");
        if (partBadgeOpt.isPresent()) {
            RewardCatalog partBadge = partBadgeOpt.get();
            for (Account user : winners) {
                Optional<AccountReward> existingPart = accountRewardRepository
                        .findByAccountIdAndRewardCatalogId(user.getId(), partBadge.getId());
                if (existingPart.isEmpty()) {
                    AccountReward partReward = AccountReward.builder()
                            .account(user)
                            .rewardCatalog(partBadge)
                            .status("UNLOCKED")
                            .progressValue(100)
                            .unlockedAt(Instant.now())
                            .build();
                    accountRewardRepository.save(partReward);
                    
                    user.setBadgeCount((user.getBadgeCount() != null ? user.getBadgeCount() : 0) + 1);
                    accountRepository.save(user);
                    log.info("Awarded TOURNAMENT_PARTICIPANT badge to user: {}", user.getEmail());
                }
            }
        }

        // Tự động xoay tua sang Giải Đấu mới cho tuần kế tiếp
        String nextWeekDate = LocalDate.now().toString();
        Tournament nextTournament = Tournament.builder()
                .name("Giải Đấu Tuần - Bắt đầu từ " + nextWeekDate)
                .description("Giải đấu thi đua hàng tuần dành cho tất cả học viên SpeakVN Journey.")
                .type("WEEKLY")
                .status("ACTIVE")
                .startsAt(Instant.now())
                .endsAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        
        nextTournament = tournamentRepository.save(nextTournament);
        log.info("Created and activated new weekly tournament: {}", nextTournament.getName());

        Map<String, Object> report = new HashMap<>();
        report.put("finalizedTournament", tournament != null ? tournament.getName() : "None (Global Fallback)");
        report.put("champion", !winners.isEmpty() ? winners.get(0).getFullName() + " (" + winners.get(0).getEmail() + ")" : "N/A");
        report.put("runnerUp", winners.size() > 1 ? winners.get(1).getFullName() + " (" + winners.get(1).getEmail() + ")" : "N/A");
        report.put("thirdPlace", winners.size() > 2 ? winners.get(2).getFullName() + " (" + winners.get(2).getEmail() + ")" : "N/A");
        report.put("newTournament", nextTournament.getName());
        report.put("newTournamentStartsAt", nextTournament.getStartsAt());
        report.put("newTournamentEndsAt", nextTournament.getEndsAt());
        report.put("message", "Chốt giải đấu và trao thưởng thành công, giải đấu mới đã được kích hoạt.");

        return report;
    }
}
