package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.LearnerDashboardResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountDashboardSummary;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountDashboardSummaryRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.SessionDetail;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.SessionDetailRepository;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnerDashboardService {

    private final AccountDashboardSummaryRepository dashboardSummaryRepository;
    private final AccountRepository accountRepository;
    private final SessionDetailRepository sessionDetailRepository;
    private final LearningUnitRepository learningUnitRepository;
    private final AccountLearningUnitRepository accountLearningUnitRepository;
    private final QuestService questService;
    private final ObjectMapper objectMapper;

    public LearnerDashboardResponse getDashboardData(String userEmail) {
        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        UUID accountId = account.getId();

        // 1. Determine Region (Dialect)
        String userRegion = normalizeRegion(account.getRegion());
        LearningUnit dialect = learningUnitRepository.findByTypeAndNameIgnoreCase("DIALECT", userRegion)
                .orElse(null);

        // 2. Fetch Levels and Progress
        String nextLessonTitle = "Khám phá lộ trình";
        String nextLessonDesc = "Chọn một vùng miền để bắt đầu";
        String nextLessonId = "1";
        int progressPercent = 0;

        if (dialect != null) {
            List<LearningUnit> levels = learningUnitRepository.findByParentIdAndType(dialect.getId(), "LEVEL");
            List<AccountLearningUnit> userProgress = accountLearningUnitRepository.findByAccountId(accountId);
            Map<UUID, AccountLearningUnit> progressMap = userProgress.stream()
                    .collect(Collectors.toMap(alu -> alu.getLearningUnit().getId(), alu -> alu));

            // Sort levels by order in metadata
            List<LearningUnit> sortedLevels = levels.stream()
                    .sorted(Comparator.comparingInt(this::getLevelOrder))
                    .collect(Collectors.toList());

            if (!sortedLevels.isEmpty()) {
                long completedCount = sortedLevels.stream()
                        .filter(l -> progressMap.containsKey(l.getId()) && progressMap.get(l.getId()).getIsCompleted())
                        .count();
                
                progressPercent = (int) ((completedCount * 100) / sortedLevels.size());

                // Find next incomplete
                LearningUnit nextUnit = sortedLevels.stream()
                        .filter(l -> !progressMap.containsKey(l.getId()) || !progressMap.get(l.getId()).getIsCompleted())
                        .findFirst()
                        .orElse(sortedLevels.get(sortedLevels.size() - 1)); // Fallback to last if all done

                nextLessonTitle = nextUnit.getName();
                nextLessonDesc = dialect.getName(); 
                
                // CRITICAL: Must return a QUIZ ID, not a LEVEL ID for the frontend to load correctly
                List<LearningUnit> quizzes = learningUnitRepository.findByParentAndType(nextUnit, "QUIZ");
                if (!quizzes.isEmpty()) {
                    // Sort quizzes by orderIndex in metadata
                    quizzes.sort(Comparator.comparingInt(this::getLevelOrder)); // reuse getLevelOrder which parses order
                    nextLessonId = quizzes.get(0).getId().toString();
                } else {
                    nextLessonId = nextUnit.getId().toString();
                }
            }
        }

        // Fetch O(1) from materialized summary table for stats
        AccountDashboardSummary summary = dashboardSummaryRepository.findByAccountId(accountId)
                .orElseGet(() -> AccountDashboardSummary.builder()
                        .accountId(accountId)
                        .totalXp(account.getTotalExperience())
                        .currentStreak(account.getCurrentStreakDays())
                        .totalLives(5)
                        .listeningScore(BigDecimal.ZERO)
                        .speakingScore(BigDecimal.ZERO)
                        .readingScore(BigDecimal.ZERO)
                        .vocabularyScore(BigDecimal.ZERO)
                        .pronunciationScore(BigDecimal.ZERO)
                        .build());

        // Construct response
        return LearnerDashboardResponse.builder()
                .currentLesson(LearnerDashboardResponse.CurrentLesson.builder()
                        .title(nextLessonTitle)
                        .description(nextLessonDesc)
                        .progress(progressPercent)
                        .id(nextLessonId)
                        .build())
                .stats(LearnerDashboardResponse.GamificationStats.builder()
                        .streakDays(summary.getCurrentStreak())
                        .xp(summary.getTotalXp())
                        .lives(summary.getTotalLives())
                        .build())
                .skillData(List.of(
                        LearnerDashboardResponse.SkillData.builder().subject("Nghe").val(summary.getListeningScore()).fullMark(BigDecimal.TEN).build(),
                        LearnerDashboardResponse.SkillData.builder().subject("Nói").val(summary.getSpeakingScore()).fullMark(BigDecimal.TEN).build(),
                        LearnerDashboardResponse.SkillData.builder().subject("Phát âm").val(summary.getPronunciationScore()).fullMark(BigDecimal.TEN).build(),
                        LearnerDashboardResponse.SkillData.builder().subject("Ngữ pháp").val(summary.getVocabularyScore()).fullMark(BigDecimal.TEN).build(),
                        LearnerDashboardResponse.SkillData.builder().subject("Đọc").val(summary.getReadingScore()).fullMark(BigDecimal.TEN).build()
                ))
                .dailyQuests(questService.getCurrentQuestsForUser(userEmail))
                .build();
    }

    private String normalizeRegion(String region) {
        if (region == null) return "SOUTH";
        String r = region.toUpperCase();
        if (r.contains("BẮC") || r.contains("NORTH") || r.contains("BAC")) return "NORTH";
        if (r.contains("TRUNG") || r.contains("CENTRAL")) return "CENTRAL";
        return "SOUTH";
    }

    private int getLevelOrder(LearningUnit unit) {
        try {
            if (unit.getMetadataJson() == null) return 0;
            Map<String, Object> meta = objectMapper.readValue(unit.getMetadataJson(), Map.class);
            Object order = meta.get("level_order");
            if (order == null) order = meta.get("orderIndex"); // Support both formats
            
            if (order instanceof Number) return ((Number) order).intValue();
            if (order instanceof String) return Integer.parseInt((String) order);
        } catch (Exception e) {
            // fallback
        }
        return 0;
    }
}
