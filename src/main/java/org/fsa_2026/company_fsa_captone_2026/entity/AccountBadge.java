package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * AccountBadge Entity (ISO Compliant)
 * Table: account_badge (singular)
 * Many-to-Many relationship between Account and Badge
 */
@Entity
@Table(name = "account_badge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(AccountBadge.AccountBadgeId.class)
public class AccountBadge implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "earned_at", nullable = false)
    @Builder.Default
    private Instant earnedAt = Instant.now();

    /**
     * Composite Primary Key Class
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class AccountBadgeId implements Serializable {
        private UUID account;
        private UUID badge;
    }
}

