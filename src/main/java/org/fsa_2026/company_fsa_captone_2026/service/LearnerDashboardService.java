package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.LearnerDashboardResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountDashboardSummary;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountDashboardSummaryRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnerDashboardService {

    private final AccountDashboardSummaryRepository dashboardSummaryRepository;
    private final AccountRepository accountRepository;
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

        // 2. Fetch stats once
        AccountDashboardSummary summary = getSummary(accountId, account);

        // Default currentLesson for users without dialect / no levels
        LearnerDashboardResponse.CurrentLesson currentLesson = LearnerDashboardResponse.CurrentLesson.builder()
                .title("Khám phá lộ trình")
                .description("Chọn một vùng miền để bắt đầu")
                .progress(0)
                .id("1")
                .isLocked(false)
                .starsNeeded(0)
                .currentStars(0)
                .levelName("")
                .build();

        if (dialect != null) {
            List<LearningUnit> levels = learningUnitRepository.findByParentIdAndType(dialect.getId(), "LEVEL");
            List<AccountLearningUnit> userProgress = accountLearningUnitRepository.findByAccountIdWithLearningUnit(accountId);
            Map<UUID, AccountLearningUnit> progressMap = userProgress.stream()
                    .collect(Collectors.toMap(
                            alu -> alu.getLearningUnit().getId(),
                            alu -> alu,
                            (a, b) -> a
                    ));

            // Sort levels by order
            List<LearningUnit> sortedLevels = levels.stream()
                    .sorted(Comparator.comparingInt(this::getLevelOrder))
                    .collect(Collectors.toList());

            if (!sortedLevels.isEmpty()) {
                long completedCount = sortedLevels.stream()
                        .filter(l -> progressMap.containsKey(l.getId()) && progressMap.get(l.getId()).getIsCompleted())
                        .count();
                int progressPercent = (int) ((completedCount * 100) / sortedLevels.size());

                // Find next incomplete level (or last as fallback when all done)
                LearningUnit nextUnit = sortedLevels.stream()
                        .filter(l -> !progressMap.containsKey(l.getId()) || !progressMap.get(l.getId()).getIsCompleted())
                        .findFirst()
                        .orElse(sortedLevels.get(sortedLevels.size() - 1));

                // ── LOCK STATUS ──────────────────────────────────────────────
                // Check if nextUnit requires stars earned in the previous level
                int minStarsRequired = getMinStarsRequired(nextUnit);
                int currentStarsInPrevLevel = 0;
                boolean isLocked = false;

                if (minStarsRequired > 0) {
                    int nextUnitIdx = sortedLevels.indexOf(nextUnit);
                    if (nextUnitIdx > 0) {
                        LearningUnit prevLevel = sortedLevels.get(nextUnitIdx - 1);
                        List<LearningUnit> prevQuizzes =
                                learningUnitRepository.findByParentIdAndType(prevLevel.getId(), "QUIZ");
                        currentStarsInPrevLevel = prevQuizzes.stream()
                                .mapToInt(q -> {
                                    AccountLearningUnit alu = progressMap.get(q.getId());
                                    return alu != null && alu.getStarsEarned() != null ? alu.getStarsEarned() : 0;
                                })
                                .sum();
                    }
                    isLocked = currentStarsInPrevLevel < minStarsRequired;
                }

                // ── TARGET QUIZ ──────────────────────────────────────────────
                // Pick the first incomplete quiz inside nextUnit
                List<LearningUnit> quizzes = learningUnitRepository.findByParentAndType(nextUnit, "QUIZ");
                quizzes.sort(Comparator.comparingInt(this::getLevelOrder));

                String quizTitle = nextUnit.getName();
                String nextLessonId = nextUnit.getId().toString(); // fallback if no quizzes

                if (!quizzes.isEmpty()) {
                    LearningUnit targetQuiz = quizzes.stream()
                            .filter(q -> !progressMap.containsKey(q.getId())
                                    || !progressMap.get(q.getId()).getIsCompleted())
                            .findFirst()
                            .orElse(quizzes.get(0)); // All completed → replay first
                    nextLessonId = targetQuiz.getId().toString();
                    quizTitle = targetQuiz.getName();
                }

                currentLesson = LearnerDashboardResponse.CurrentLesson.builder()
                        .title(quizTitle)
                        .description(dialect.getName())
                        .progress(progressPercent)
                        .id(nextLessonId)
                        .isLocked(isLocked)
                        .starsNeeded(isLocked ? minStarsRequired : 0)
                        .currentStars(currentStarsInPrevLevel)
                        .levelName(nextUnit.getName())
                        .build();
            }
        }

        return LearnerDashboardResponse.builder()
                .currentLesson(currentLesson)
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

    // ── Helper: fetch/build stats summary ────────────────────────────────────
    private AccountDashboardSummary getSummary(UUID accountId, Account account) {
        return dashboardSummaryRepository.findByAccountId(accountId)
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
    }

    // ── Helper: parse min_stars_required from level metadata ──────────────────
    private int getMinStarsRequired(LearningUnit level) {
        if (level.getMetadataJson() == null) return 0;
        try {
            Map<String, Object> meta = objectMapper.readValue(level.getMetadataJson(), Map.class);
            Object val = meta.get("min_stars_required");
            if (val instanceof Number) return ((Number) val).intValue();
            if (val instanceof String) return Integer.parseInt((String) val);
        } catch (Exception ignored) {}
        return 0;
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
            if (order == null) order = meta.get("orderIndex");
            if (order instanceof Number) return ((Number) order).intValue();
            if (order instanceof String) return Integer.parseInt((String) order);
        } catch (Exception ignored) {}
        return 0;
    }
}
