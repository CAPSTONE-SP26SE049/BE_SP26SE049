package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.LearnerDashboardResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountDashboardSummary;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountDashboardSummaryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearnerDashboardService {

    private final AccountDashboardSummaryRepository dashboardSummaryRepository;

    public LearnerDashboardResponse getDashboardData(UUID accountId) {
        // Fetch O(1) from materialized summary table
        AccountDashboardSummary summary = dashboardSummaryRepository.findByAccountId(accountId)
                .orElseGet(() -> AccountDashboardSummary.builder()
                        .accountId(accountId)
                        .totalXp(0)
                        .currentStreak(0)
                        .totalLives(5)
                        .listeningScore(BigDecimal.ZERO)
                        .speakingScore(BigDecimal.ZERO)
                        .readingScore(BigDecimal.ZERO)
                        .vocabularyScore(BigDecimal.ZERO)
                        .pronunciationScore(BigDecimal.ZERO)
                        .currentLessonTitle("Bắt đầu khám phá khóa học")
                        .currentLessonDesc("Chọn một vùng miền để luyện tập")
                        .currentLessonProgress(0)
                        .build());

        // Construct response
        return LearnerDashboardResponse.builder()
                .currentLesson(LearnerDashboardResponse.CurrentLesson.builder()
                        .title(summary.getCurrentLessonTitle() != null ? summary.getCurrentLessonTitle() : "Bắt đầu bài học mới")
                        .description(summary.getCurrentLessonDesc() != null ? summary.getCurrentLessonDesc() : "Khám phá lộ trình")
                        .progress(summary.getCurrentLessonProgress())
                        .id("1") // Could be bound to a real ID later
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
                .dailyQuests(List.of(
                        // Mock dynamic quests based on user behavior (could be expanded)
                        LearnerDashboardResponse.DailyQuest.builder().title("Hoàn thành 1 bài luyện nói").xp("+50 XP").done(summary.getSpeakingScore().compareTo(BigDecimal.ZERO) > 0).progress(100).build(),
                        LearnerDashboardResponse.DailyQuest.builder().title("Duy trì Streak 3 ngày liên tiếp").xp("+100 XP").done(summary.getCurrentStreak() >= 3).progress(Math.min(100, (summary.getCurrentStreak() * 100) / 3)).build(),
                        LearnerDashboardResponse.DailyQuest.builder().title("Tham gia 1 bài kiểm tra tổng hợp").xp("+150 XP").done(false).progress(0).build()
                ))
                .build();
    }
}
