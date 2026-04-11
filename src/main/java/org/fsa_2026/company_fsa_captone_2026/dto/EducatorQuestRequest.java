package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EducatorQuestRequest {
    private String title;
    private String description;
    private Integer xpReward;
    private Integer targetValue;
    private String questType;
    private LocalDateTime endDate;
}
