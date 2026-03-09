package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Tournament Entity
 * Represents a competitive tournament event (weekly, monthly, or special).
 *
 * Table: tournament
 * FE-05: Leaderboard System with weekly tournaments and social competition
 */
@Entity
@Table(name = "tournament")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Type of tournament.
     * Values: WEEKLY / MONTHLY / SPECIAL
     */
    @Column(name = "type", nullable = false, length = 30)
    @Builder.Default
    private String type = "WEEKLY";

    /**
     * Current status of the tournament.
     * Values: UPCOMING / ACTIVE / FINISHED / CANCELLED
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "UPCOMING";

    /** Optional: dialect scope for the tournament */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dialect_id", foreignKey = @ForeignKey(name = "fk_tournament_dialect"))
    private Dialect dialect;

    /** Optional: region scope. Matches account.region */
    @Column(name = "region_code", length = 30)
    private String regionCode;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    /** Maximum number of participants allowed (null = unlimited) */
    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    /**
     * Prize configuration JSON.
     * Example: {"1": {"xp": 5000, "badge": "TOURNAMENT_WINNER"}, "2": {"xp": 2500}}
     */
    @Column(name = "prize_config_json", columnDefinition = "jsonb")
    private String prizeConfigJson;
}
