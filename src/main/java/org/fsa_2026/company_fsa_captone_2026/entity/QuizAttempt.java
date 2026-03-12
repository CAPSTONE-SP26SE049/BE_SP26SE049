package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.fsa_2026.company_fsa_captone_2026.entity.enums.QuizType;
import java.time.Instant;

/**
 * Quiz Attempt Entity
 * Table: quiz_attempt - Records learner attempts on educator-created quizzes
 */
@Entity
@Table(name = "quiz_attempt")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class QuizAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Account student;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "completed", nullable = false)
    private Boolean completed;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Optional: Detailed breakdown by question/section
    @Column(name = "answers_snapshot", columnDefinition = "TEXT")
    private String answersSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_type", length = 20)
    private QuizType quizType; // e.g., PRACTICE, ASSESSMENT, DIAGNOSTIC

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;
}