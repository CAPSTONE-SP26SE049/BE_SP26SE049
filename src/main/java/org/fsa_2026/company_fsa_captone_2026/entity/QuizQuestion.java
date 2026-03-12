package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.DifficultyLevel;

/**
 * Quiz Question Entity
 * Table: quiz_question - Holds individual questions for an input test
 */
@Entity
@Table(name = "quiz_question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class QuizQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "skill_type", nullable = false, length = 50)
    private String skillType; // LISTENING, SPEAKING, READING, WRITING

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 20)
    private DifficultyLevel difficulty;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "points", nullable = false)
    private Integer points;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;
}
