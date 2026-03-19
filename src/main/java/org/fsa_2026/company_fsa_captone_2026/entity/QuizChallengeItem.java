package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "quiz_challenge_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizChallengeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "challenge_bank_id", nullable = false)
    private UUID challengeBankId;

    @Column(name = "order_index")
    private Integer orderIndex;
}
