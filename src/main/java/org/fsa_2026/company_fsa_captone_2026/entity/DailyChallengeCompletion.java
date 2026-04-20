package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

/**
 * DailyChallengeCompletion Entity - Tracks users who completed the daily challenge.
 */
@Entity
@Table(name = "daily_challenge_completion", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_id", "daily_challenge_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChallengeCompletion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_challenge_id", nullable = false)
    private DailyChallenge dailyChallenge;

    @Column(name = "completed_at", nullable = false)
    @Builder.Default
    private OffsetDateTime completedAt = OffsetDateTime.now();
}
