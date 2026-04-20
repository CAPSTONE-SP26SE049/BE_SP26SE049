package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChallengeResponse {
    private UUID challengeId;
    private String contentText;
    private SkillType skillType;
    private String difficultyTag;
    private String region;
    private Map<String, Object> metadataJson;
}
