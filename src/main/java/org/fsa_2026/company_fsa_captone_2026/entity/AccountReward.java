package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * AccountReward Entity - Super Entity gộp AccountAchievement + AccountBadge
 *
 * Table: account_reward
 * Theo dõi các phần thưởng (achievement / badge) mà người dùng đã mở khóa hoặc đang tiến tới.
 */
@Entity
@Table(name = "account_reward", uniqueConstraints = @UniqueConstraint(
        name = "uk_account_reward",
        columnNames = {"account_id", "reward_catalog_id"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_account_reward_account"))
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_catalog_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_account_reward_catalog"))
    private RewardCatalog rewardCatalog;

    /**
     * Trạng thái tiến độ: LOCKED, IN_PROGRESS, UNLOCKED
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "LOCKED";

    /** Tiến độ hiện tại hướng đến ngưỡng phần thưởng */
    @Column(name = "progress_value", nullable = false)
    @Builder.Default
    private int progressValue = 0;

    /** Thời điểm mở khóa phần thưởng lần đầu */
    @Column(name = "unlocked_at")
    private Instant unlockedAt;

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
