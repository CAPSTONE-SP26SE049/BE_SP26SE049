package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.DifficultyTag;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeBankRequest {
    private String contentText;
    private SkillType skillType;
    private DifficultyTag difficultyTag;
    private Boolean isGlobal;
    private Map<String, Object> metadataJson;
    private UUID createdBy;
}
