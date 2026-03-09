package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * TournamentParticipant Entity
 * Tracks a user's registration, score, and rank within a tournament.
 *
 * Table: tournament_participant
 * FE-05: Leaderboard System with weekly tournaments
 */
@Entity
@Table(name = "tournament_participant", uniqueConstraints = @UniqueConstraint(name = "uk_tournament_participant", columnNames = {
        "tournament_id", "account_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tp_tournament"))
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tp_account"))
    private Account account;

    /** Live-updated rank during the tournament */
    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "total_xp", nullable = false)
    @Builder.Default
    private int totalXp = 0;

    @Column(name = "challenges_completed", nullable = false)
    @Builder.Default
    private int challengesCompleted = 0;

    @Column(name = "average_score", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal averageScore = BigDecimal.ZERO;

    /**
     * Participation status.
     * Values: REGISTERED / ACTIVE / COMPLETED / DISQUALIFIED
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "REGISTERED";

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = Instant.now();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
