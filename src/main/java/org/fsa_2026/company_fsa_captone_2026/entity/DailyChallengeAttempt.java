package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_challenge_attempt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChallengeAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private ChallengeBank challenge;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "dialect", length = 20)
    private String dialect;
}
