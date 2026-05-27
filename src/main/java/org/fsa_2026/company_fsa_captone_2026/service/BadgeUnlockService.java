package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountReward;
import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRewardRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.FriendshipRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.RewardCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeUnlockService {

    private final AccountRepository accountRepository;
    private final RewardCatalogRepository rewardCatalogRepository;
    private final AccountRewardRepository accountRewardRepository;
    private final FriendshipRepository friendshipRepository;
    private final AccountLearningUnitRepository accountLearningUnitRepository;

    /**
     * Quét và tự động mở khóa các Badge cho học viên khi đạt cột mốc.
     */
    @Transactional
    public void checkAndUnlockBadges(String email) {
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) {
            return;
        }
        checkAndUnlockBadges(accountOpt.get());
    }

    /**
     * Quét và tự động mở khóa các Badge cho học viên khi đạt cột mốc.
     */
    @Transactional
    public void checkAndUnlockBadges(Account account) {
        try {
            int totalStars = account.getTotalStars() != null ? account.getTotalStars() : 0;
            int currentStreak = account.getCurrentStreakDays() != null ? account.getCurrentStreakDays() : 0;
            long friendCount = friendshipRepository.countAcceptedByAccount(account.getId());
            long completedQuizzes = accountLearningUnitRepository.countByAccountIdAndIsCompletedTrue(account.getId());

            log.debug("[BadgeUnlock] Checking badges for user={}: stars={}, streak={}, friends={}, quizzes={}",
                    account.getEmail(), totalStars, currentStreak, friendCount, completedQuizzes);

            // 1. Kiểm tra Badge nhóm Sao tích lũy (SCORE)
            checkBadge(account, "SCORE_FIRST_STAR", totalStars >= 1);
            checkBadge(account, "SCORE_50_STARS", totalStars >= 50);
            checkBadge(account, "SCORE_100_STARS", totalStars >= 100);
            checkBadge(account, "SCORE_300_STARS", totalStars >= 300);
            checkBadge(account, "SCORE_500_STARS", totalStars >= 500);

            // 2. Kiểm tra Badge nhóm Bạn bè (SOCIAL)
            checkBadge(account, "SOCIAL_FIRST_FRIEND", friendCount >= 1);
            checkBadge(account, "SOCIAL_5_FRIENDS", friendCount >= 5);
            checkBadge(account, "SOCIAL_10_FRIENDS", friendCount >= 10);

            // 3. Kiểm tra Badge nhóm Streak (STREAK)
            checkBadge(account, "STREAK_3_DAYS", currentStreak >= 3);
            checkBadge(account, "STREAK_7_DAYS", currentStreak >= 7);
            checkBadge(account, "STREAK_15_DAYS", currentStreak >= 15);
            checkBadge(account, "STREAK_30_DAYS", currentStreak >= 30);

            // 4. Kiểm tra Badge nhóm Học tập (LEARNING)
            checkBadge(account, "LEARN_FIRST_LEVEL", completedQuizzes >= 1);
            checkBadge(account, "LEARN_5_LEVELS", completedQuizzes >= 5);
            checkBadge(account, "LEARN_10_LEVELS", completedQuizzes >= 10);
            checkBadge(account, "LEARN_15_LEVELS", completedQuizzes >= 15);

        } catch (Exception e) {
            log.error("[BadgeUnlock] Lỗi khi quét tự động mở khóa badge cho học viên {}: {}", 
                    account.getEmail(), e.getMessage(), e);
        }
    }

    private void checkBadge(Account account, String badgeCode, boolean isConditionMet) {
        if (!isConditionMet) {
            return;
        }

        Optional<RewardCatalog> badgeOpt = rewardCatalogRepository.findByCode(badgeCode);
        if (badgeOpt.isEmpty()) {
            return;
        }

        RewardCatalog badge = badgeOpt.get();
        Optional<AccountReward> existingReward = accountRewardRepository
                .findByAccountIdAndRewardCatalogId(account.getId(), badge.getId());

        if (existingReward.isEmpty()) {
            AccountReward ar = AccountReward.builder()
                    .account(account)
                    .rewardCatalog(badge)
                    .status("UNLOCKED")
                    .progressValue(100)
                    .unlockedAt(Instant.now())
                    .build();
            accountRewardRepository.save(ar);

            // Tăng số lượng huy hiệu trên hồ sơ tài khoản
            int currentBadgeCount = account.getBadgeCount() != null ? account.getBadgeCount() : 0;
            account.setBadgeCount(currentBadgeCount + 1);
            accountRepository.save(account);

            log.info("[BadgeUnlock] Tự động mở khóa Huy hiệu '{}' thành công cho học viên '{}'. BadgeCount mới: {}",
                    badge.getName(), account.getEmail(), account.getBadgeCount());
        }
    }
}
