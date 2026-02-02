package com.aiservice.domain.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Builder.Default
    @Column(name = "total_stars")
    private int totalStars = 0;

    @Builder.Default
    @Column(name = "current_streak")
    private int currentStreak = 0;

    @Builder.Default
    @Column(name = "total_xp")
    private int totalXp = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "native_region")
    private RegionType nativeRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_region")
    private RegionType targetRegion;
}
