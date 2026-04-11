package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.LearnerDashboardResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountQuestProgress;
import org.fsa_2026.company_fsa_captone_2026.entity.Quest;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountQuestProgressRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.QuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestService {

    private final QuestRepository questRepository;
    private final AccountQuestProgressRepository progressRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<LearnerDashboardResponse.DailyQuest> getCurrentQuestsForUser(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        LocalDate today = LocalDate.now();
        List<Quest> globalQuests = questRepository.findByIsActiveTrueAndStudentIdIsNull();
        List<Quest> personalQuests = questRepository.findByIsActiveTrueAndStudentId(account.getId());
        List<Quest> allQuests = Stream.concat(globalQuests.stream(), personalQuests.stream())
                .filter(q -> q.getEndDate() == null || q.getEndDate().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());

        List<AccountQuestProgress> userProgress = progressRepository.findByAccountIdAndQuestDate(account.getId(), today);

        return allQuests.stream().map(quest -> {
            Optional<AccountQuestProgress> progressOpt = userProgress.stream()
                    .filter(p -> p.getQuest().getId().equals(quest.getId()))
                    .findFirst();

            int currentVal = progressOpt.map(AccountQuestProgress::getCurrentValue).orElse(0);
            boolean isDone = progressOpt.map(AccountQuestProgress::getIsCompleted).orElse(false);
            int progressPercent = (int) Math.min(100, (currentVal * 100.0) / quest.getTargetValue());

            return LearnerDashboardResponse.DailyQuest.builder()
                    .title(quest.getTitle())
                    .xp("+" + quest.getXpReward() + " XP")
                    .done(isDone)
                    .progress(progressPercent)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateQuestProgress(UUID accountId, String questType, int delta) {
        LocalDate today = LocalDate.now();
        List<Quest> relevantQuests = questRepository.findByQuestTypeAndIsActiveTrue(questType);

        for (Quest quest : relevantQuests) {
            AccountQuestProgress progress = progressRepository
                    .findByAccountIdAndQuestIdAndQuestDate(accountId, quest.getId(), today)
                    .orElseGet(() -> {
                        Account account = accountRepository.findById(accountId).orElseThrow();
                        return AccountQuestProgress.builder()
                                .account(account)
                                .quest(quest)
                                .questDate(today)
                                .currentValue(0)
                                .isCompleted(false)
                                .build();
                    });

            if (Boolean.TRUE.equals(progress.getIsCompleted())) continue;

            progress.setCurrentValue(progress.getCurrentValue() + delta);
            if (progress.getCurrentValue() >= quest.getTargetValue()) {
                progress.setIsCompleted(true);
                progress.setCompletedAt(LocalDateTime.now());
                grantQuestReward(progress.getAccount(), quest);
            }
            progressRepository.save(progress);
        }
    }

    private void grantQuestReward(Account account, Quest quest) {
        int currentXp = account.getTotalExperience() != null ? account.getTotalExperience() : 0;
        account.setTotalExperience(currentXp + quest.getXpReward());
        accountRepository.save(account);
        log.info("Granted {} XP to user {} for completing quest: {}", 
            quest.getXpReward(), account.getEmail(), quest.getTitle());
    }

    // Educator CRUD
    @Transactional
    public Quest createQuest(Quest quest) {
        return questRepository.save(quest);
    }

    @Transactional(readOnly = true)
    public List<Quest> getAllQuests() {
        return questRepository.findAll();
    }
}
