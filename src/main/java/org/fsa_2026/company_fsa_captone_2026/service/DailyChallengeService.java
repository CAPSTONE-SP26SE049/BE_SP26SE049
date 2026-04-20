package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.DailyChallengeResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.DailyChallengeSubmissionRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.DailyChallengeSubmissionResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.*;
import org.fsa_2026.company_fsa_captone_2026.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyChallengeService extends BaseService<DailyChallenge, UUID> {

    private final DailyChallengeRepository dailyChallengeRepository;
    private final DailyChallengeCompletionRepository dailyChallengeCompletionRepository;
    private final ChallengeBankRepository challengeBankRepository;
    private final AccountRepository accountRepository;

    @Override
    protected BaseRepository<DailyChallenge, UUID> getRepository() {
        return dailyChallengeRepository;
    }

    @Override
    protected String getEntityName() {
        return "DailyChallenge";
    }

    @Transactional
    public DailyChallengeResponse getCurrentDailyChallenge() {
        LocalDate today = LocalDate.now();
        DailyChallenge dailyChallenge = dailyChallengeRepository.findByChallengeDate(today)
                .orElseGet(this::selectNewDailyChallenge);

        ChallengeBank cb = dailyChallenge.getChallenge();
        return DailyChallengeResponse.builder()
                .challengeId(dailyChallenge.getId())
                .contentText(cb.getContentText())
                .skillType(cb.getSkillType())
                .difficultyTag(cb.getDifficultyTag().name())
                .region(cb.getRegion())
                .metadataJson(cb.getMetadataJson())
                .build();
    }

    @Transactional
    public DailyChallengeSubmissionResponse submitSolution(DailyChallengeSubmissionRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        DailyChallenge dailyChallenge = dailyChallengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new RuntimeException("Thử thách không tồn tại"));

        if (dailyChallengeCompletionRepository.existsByAccountIdAndDailyChallengeId(account.getId(), dailyChallenge.getId())) {
            return DailyChallengeSubmissionResponse.builder()
                    .correct(true)
                    .feedback("Bạn đã hoàn thành thử thách này hôm nay!")
                    .streak(account.getCurrentStreakDays())
                    .build();
        }

        ChallengeBank cb = dailyChallenge.getChallenge();
        Object correctAnswerObj = cb.getMetadataJson().get("correctAnswer");
        if (correctAnswerObj == null) {
            throw new RuntimeException("Câu hỏi này không có đáp án được thiết lập trong metadata");
        }
        
        String correctAnswer = correctAnswerObj.toString();
        boolean isCorrect = request.getUserResponse().trim().equalsIgnoreCase(correctAnswer.trim());

        if (isCorrect) {
            // Update streak and stars
            updateUserStreakAndRewards(account);
            
            // Record completion
            DailyChallengeCompletion completion = DailyChallengeCompletion.builder()
                    .account(account)
                    .dailyChallenge(dailyChallenge)
                    .build();
            dailyChallengeCompletionRepository.save(completion);

            return DailyChallengeSubmissionResponse.builder()
                    .correct(true)
                    .score(100.0)
                    .bonusTokens(50)
                    .streak(account.getCurrentStreakDays())
                    .feedback("Chính xác! Bạn nhận được 50 stars.")
                    .build();
        } else {
            return DailyChallengeSubmissionResponse.builder()
                    .correct(false)
                    .feedback("Chưa chính xác, hãy thử lại!")
                    .streak(account.getCurrentStreakDays())
                    .build();
        }
    }

    private void updateUserStreakAndRewards(Account account) {
        LocalDate today = LocalDate.now();
        LocalDate lastDate = account.getLastLoginDate();

        if (lastDate == null) {
            account.setCurrentStreakDays(1);
        } else if (lastDate.equals(today.minusDays(1))) {
            account.setCurrentStreakDays(account.getCurrentStreakDays() + 1);
        } else if (!today.equals(lastDate)) {
            account.setCurrentStreakDays(1);
        }
        
        account.setLastLoginDate(today);
        account.setTotalStars(account.getTotalStars() + 50);
        accountRepository.save(account);
    }

    private DailyChallenge selectNewDailyChallenge() {
        List<ChallengeBank> allChallenges = challengeBankRepository.findAll();
        if (allChallenges.isEmpty()) {
            throw new RuntimeException("Kho câu hỏi trống, không thể tạo thử thách hàng ngày");
        }
        
        ChallengeBank randomCB = allChallenges.get(new Random().nextInt(allChallenges.size()));
        
        DailyChallenge newChallenge = DailyChallenge.builder()
                .challengeDate(LocalDate.now())
                .challenge(randomCB)
                .skillType(randomCB.getSkillType())
                .build();
        
        return dailyChallengeRepository.save(newChallenge);
    }
}
