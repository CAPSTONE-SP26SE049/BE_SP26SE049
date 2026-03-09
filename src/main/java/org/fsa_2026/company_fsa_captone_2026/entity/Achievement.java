package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Achievement Entity
 * Represents a milestone/accomplishment that users can unlock.
 *
 * Table: achievement
 * FE-04: Achievement and Badge System
 */
@Entity
@Table(name = "achievement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Category of achievement.
     * Values: STREAK, SCORE, LEARNING, CHALLENGE, SOCIAL, SPECIAL, GENERAL
     */
    @Column(name = "category", nullable = false, length = 50)
    @Builder.Default
    private String category = "GENERAL";

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /**
     * Criteria JSON defining when this achievement is unlocked.
     * Example: {"type": "streak_days", "threshold": 7}
     */
    @Column(name = "criteria_json", nullable = false, columnDefinition = "jsonb")
    private String criteriaJson;

    @Column(name = "xp_reward", nullable = false)
    @Builder.Default
    private int xpReward = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
