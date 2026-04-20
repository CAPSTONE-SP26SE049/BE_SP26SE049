package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RewardType;

/**
 * RewardCatalog Entity - Super Entity gộp Achievement + Badge
 *
 * Table: reward_catalog
 * Phân biệt loại phần thưởng qua field reward_type (ACHIEVEMENT / BADGE)
 * Điều kiện unlock: vượt qua quiz được gắn thành tựu này.
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

    /**
     * Loại phần thưởng: ACHIEVEMENT hoặc BADGE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 20)
    @Builder.Default
    private RewardType rewardType = RewardType.ACHIEVEMENT;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "xp_reward", nullable = false)
    @Builder.Default
    private int xpReward = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
