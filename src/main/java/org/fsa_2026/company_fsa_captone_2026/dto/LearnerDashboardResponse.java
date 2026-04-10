package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LearnerDashboardResponse {
    private CurrentLesson currentLesson;
    private GamificationStats stats;
    private List<DailyQuest> dailyQuests;
    private List<SkillData> skillData;

    @Data
    @Builder
    public static class CurrentLesson {
        private String title;
        private String description;
        private Integer progress;
        private String id;
    }

    @Data
    @Builder
    public static class GamificationStats {
        private Integer streakDays;
        private Integer xp;
        private Integer lives;
    }

    @Data
    @Builder
    public static class DailyQuest {
        private String title;
        private String xp;
        private Boolean done;
        private Integer progress;
    }

    @Data
    @Builder
    public static class SkillData {
        private String subject;
        private BigDecimal val;
        private BigDecimal fullMark;
    }
}
