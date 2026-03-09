package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * LeaderboardEntry Entity
 * Represents a user's rank and score within a specific leaderboard.
 *
 * Table: leaderboard_entry
 * FE-05: Leaderboard System
 */
@Entity
@Table(name = "leaderboard_entry", uniqueConstraints = @UniqueConstraint(name = "uk_leaderboard_entry", columnNames = {
        "leaderboard_id", "account_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leaderboard_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lb_entry_leaderboard"))
    private Leaderboard leaderboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lb_entry_account"))
    private Account account;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    @Column(name = "total_xp", nullable = false)
    @Builder.Default
    private int totalXp = 0;

    @Column(name = "total_stars", nullable = false)
    @Builder.Default
    private int totalStars = 0;

    @Column(name = "challenges_completed", nullable = false)
    @Builder.Default
    private int challengesCompleted = 0;

    @Column(name = "average_score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal averageScore = BigDecimal.ZERO;

    /** Streak days at the time of this snapshot */
    @Column(name = "streak_days", nullable = false)
    @Builder.Default
    private int streakDays = 0;

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
