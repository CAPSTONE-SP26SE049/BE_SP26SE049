package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * DailyChallenge Entity
 * Represents a challenge designated as the daily challenge for a specific date.
 *
 * Table: daily_challenge
 * FE-04: Achievement and Badge System (Daily Challenges & Streak Tracking)
 */
@Entity
@Table(name = "daily_challenge", uniqueConstraints = @UniqueConstraint(name = "uk_daily_challenge_date_challenge", columnNames = {
        "challenge_date", "challenge_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChallenge extends BaseEntity {

    @Column(name = "challenge_date", nullable = false)
    private LocalDate challengeDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false, foreignKey = @ForeignKey(name = "fk_daily_challenge_challenge"))
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dialect_id", foreignKey = @ForeignKey(name = "fk_daily_challenge_dialect"))
    private Dialect dialect;

    /**
     * Difficulty of this daily challenge.
     * Values: EASY / MEDIUM / HARD
     */
    @Column(name = "difficulty", nullable = false, length = 20)
    @Builder.Default
    private String difficulty = "MEDIUM";

    /** Bonus XP awarded for completing this daily challenge */
    @Column(name = "bonus_xp", nullable = false)
    @Builder.Default
    private int bonusXp = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
