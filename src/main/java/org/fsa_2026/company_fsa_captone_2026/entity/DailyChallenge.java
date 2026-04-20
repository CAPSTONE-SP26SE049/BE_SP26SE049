package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;
import java.time.LocalDate;

/**
 * DailyChallenge Entity - Links a challenge to a specific date for all students.
 */
@Entity
@Table(name = "daily_challenge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChallenge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_bank_id", nullable = false)
    private ChallengeBank challenge;

    @Column(name = "challenge_date", nullable = false, unique = true)
    private LocalDate challengeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, length = 20)
    private SkillType skillType;
}
