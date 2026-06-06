package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "minigame_challenge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinigameChallenge extends BaseEntity {

    @Column(name = "game_type", nullable = false, length = 50)
    private String gameType;

    @Column(name = "pair_type", nullable = false, length = 20)
    private String pairType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> questionData;
}
