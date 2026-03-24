package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RewardType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * RewardCatalog Entity - Super Entity gộp Achievement + Badge
 *
 * Table: reward_catalog
 * Phân biệt loại phần thưởng qua field reward_type (ACHIEVEMENT / BADGE)
 */
@Entity
@Table(name = "reward_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardCatalog extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Loại phần thưởng: ACHIEVEMENT hoặc BADGE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 20)
    @Builder.Default
    private RewardType rewardType = RewardType.ACHIEVEMENT;

    /**
     * Danh mục con: STREAK, SCORE, LEARNING, CHALLENGE, SOCIAL, SPECIAL, GENERAL
     */
    @Column(name = "category", nullable = false, length = 50)
    @Builder.Default
    private String category = "GENERAL";

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /**
     * Tiêu chí JSON để mở khóa phần thưởng.
     * Ví dụ: {"type": "streak_days", "threshold": 7}
     * @JdbcTypeCode(SqlTypes.JSON) → tells Hibernate to bind as JSON, not varchar
     *   so PostgreSQL accepts it into the jsonb column without a cast error.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria_json", nullable = false, columnDefinition = "jsonb")
    private String criteriaJson;

    @Column(name = "xp_reward", nullable = false)
    @Builder.Default
    private int xpReward = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
