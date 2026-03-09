package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * AccountDailyChallenge Entity
 * Tracks a user's completion status and score for each daily challenge.
 *
 * Table: account_daily_challenge
 * FE-04: Achievement and Badge System (Daily Challenges & Streak Tracking)
 */
@Entity
@Table(name = "account_daily_challenge", uniqueConstraints = @UniqueConstraint(name = "uk_account_daily_challenge", columnNames = {
        "account_id", "daily_challenge_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDailyChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adc_account"))
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_challenge_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adc_daily_challenge"))
    private DailyChallenge dailyChallenge;

    /**
     * Completion status.
     * Values: PENDING / COMPLETED / SKIPPED
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "score_achieved", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal scoreAchieved = BigDecimal.ZERO;

    @Column(name = "xp_earned", nullable = false)
    @Builder.Default
    private int xpEarned = 0;

    /** The specific attempt record linked to this daily challenge completion */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", foreignKey = @ForeignKey(name = "fk_adc_attempt"))
    private Attempt attempt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
