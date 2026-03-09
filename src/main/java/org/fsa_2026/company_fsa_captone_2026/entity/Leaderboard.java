package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Leaderboard Entity
 * Represents a ranked leaderboard for a specific scope and time period.
 *
 * Table: leaderboard
 * FE-05: Leaderboard System with regional rankings, weekly tournaments,
 * and social competition features.
 */
@Entity
@Table(name = "leaderboard")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leaderboard extends BaseEntity {

    /**
     * Scope of the leaderboard.
     * Values: GLOBAL / REGIONAL / CLASSROOM / FRIENDS
     */
    @Column(name = "scope", nullable = false, length = 30)
    @Builder.Default
    private String scope = "GLOBAL";

    /**
     * Time period type.
     * Values: DAILY / WEEKLY / MONTHLY / ALL_TIME
     */
    @Column(name = "period_type", nullable = false, length = 20)
    @Builder.Default
    private String periodType = "WEEKLY";

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /** Optional: Region code (matches account.region) for regional leaderboards */
    @Column(name = "region_code", length = 30)
    private String regionCode;

    /** Optional: classroom-scoped leaderboard */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", foreignKey = @ForeignKey(name = "fk_leaderboard_classroom"))
    private Classroom classroom;

    /** Optional: dialect-scoped leaderboard */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dialect_id", foreignKey = @ForeignKey(name = "fk_leaderboard_dialect"))
    private Dialect dialect;

    /** Once finalized, the rankings are locked and no longer updated */
    @Column(name = "is_finalized", nullable = false)
    @Builder.Default
    private boolean isFinalized = false;
}
