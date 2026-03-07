package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * UserProfile Entity (ISO Compliant)
 * Table: user_profile (singular)
 * Extended profile information (1:1 with account)
 */
@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "total_stars", nullable = false)
    @Builder.Default
    private Integer totalStars = 0;

    @Column(name = "current_streak_days", nullable = false)
    @Builder.Default
    private Integer currentStreakDays = 0;

    @Column(name = "total_experience", nullable = false)
    @Builder.Default
    private Integer totalExperience = 0;
}

