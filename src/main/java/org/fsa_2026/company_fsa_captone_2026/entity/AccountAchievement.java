package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * AccountAchievement Entity
 * Tracks which achievements a user has unlocked and their progress.
 *
 * Table: account_achievement
 * FE-04: Achievement and Badge System
 */
@Entity
@Table(name = "account_achievement", uniqueConstraints = @UniqueConstraint(name = "uk_account_achievement", columnNames = {
        "account_id", "achievement_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_acct_achievement_account"))
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false, foreignKey = @ForeignKey(name = "fk_acct_achievement_achievement"))
    private Achievement achievement;

    /**
     * Progress status.
     * Values: LOCKED, IN_PROGRESS, UNLOCKED
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "LOCKED";

    /** Current progress towards the achievement threshold */
    @Column(name = "progress_value", nullable = false)
    @Builder.Default
    private int progressValue = 0;

    /** Timestamp when the achievement was first unlocked */
    @Column(name = "unlocked_at")
    private Instant unlockedAt;

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
