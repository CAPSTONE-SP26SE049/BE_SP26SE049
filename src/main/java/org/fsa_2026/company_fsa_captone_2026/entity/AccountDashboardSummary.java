package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_dashboard_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDashboardSummary {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "total_xp")
    @Builder.Default
    private Integer totalXp = 0;

    @Column(name = "current_streak")
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "total_lives")
    @Builder.Default
    private Integer totalLives = 5;

    @Column(name = "listening_score")
    @Builder.Default
    private BigDecimal listeningScore = BigDecimal.ZERO;

    @Column(name = "speaking_score")
    @Builder.Default
    private BigDecimal speakingScore = BigDecimal.ZERO;

    @Column(name = "reading_score")
    @Builder.Default
    private BigDecimal readingScore = BigDecimal.ZERO;

    @Column(name = "vocabulary_score")
    @Builder.Default
    private BigDecimal vocabularyScore = BigDecimal.ZERO;

    @Column(name = "pronunciation_score")
    @Builder.Default
    private BigDecimal pronunciationScore = BigDecimal.ZERO;

    @Column(name = "current_lesson_title")
    private String currentLessonTitle;

    @Column(name = "current_lesson_desc")
    private String currentLessonDesc;

    @Column(name = "current_lesson_progress")
    @Builder.Default
    private Integer currentLessonProgress = 0;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

}
