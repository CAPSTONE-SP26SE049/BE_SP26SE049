package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * AccountLearningUnit Entity - Tracks user progress on specific LearningUnits (Levels).
 */
@Entity
@Table(name = "account_learning_unit", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "learning_unit_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLearningUnit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_unit_id", nullable = false)
    private LearningUnit learningUnit;

    @Column(name = "stars_earned", nullable = false)
    @Builder.Default
    private Integer starsEarned = 0; // 1-3 stars

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "highest_score", precision = 5, scale = 2)
    private BigDecimal highestScore;
}
