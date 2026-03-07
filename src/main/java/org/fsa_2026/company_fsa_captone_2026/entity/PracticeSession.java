package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * PracticeSession Entity (NEW - ISO Compliant)
 * Table: practice_session
 * Tracks user practice sessions for engagement analytics
 */
@Entity
@Table(name = "practice_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeSession {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @org.hibernate.annotations.UuidGenerator
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;
}

