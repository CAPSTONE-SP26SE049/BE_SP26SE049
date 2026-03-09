package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Friendship Entity
 * Manages social connections between users for social competition features.
 *
 * Table: friendship
 * FE-05: Leaderboard System - Social competition features
 */
@Entity
@Table(name = "friendship", uniqueConstraints = @UniqueConstraint(name = "uk_friendship_pair", columnNames = {
        "requester_id", "addressee_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private java.util.UUID id;

    /** The user who sent the friend request */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false, foreignKey = @ForeignKey(name = "fk_friendship_requester"))
    private Account requester;

    /** The user who received the friend request */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_friendship_addressee"))
    private Account addressee;

    /**
     * Friendship status.
     * Values: PENDING / ACCEPTED / DECLINED / BLOCKED
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

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
