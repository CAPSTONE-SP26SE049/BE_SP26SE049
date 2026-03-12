package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizQuestion;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionResponse implements Serializable {

    private String id;
    private String skillType;
    private String difficulty;
    private Integer questionOrder;
    private Integer points;
    private Map<String, Object> contentData;

    public static QuizQuestionResponse fromEntity(QuizQuestion entity) {
        if (entity == null) return null;
        return QuizQuestionResponse.builder()
                .id(entity.getId().toString())
                .skillType(entity.getSkillType())
                .difficulty(entity.getDifficulty() != null ? entity.getDifficulty().name() : null)
                .questionOrder(entity.getQuestionOrder())
                .points(entity.getPoints())
                .contentData(entity.getContentData())
                .build();
    }
}
