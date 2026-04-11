package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private Integer totalXp;
    private Integer currentStreak;
    private BigDecimal averageSpeakingScore;
    private List<StudentAssignmentResponse> activeAssignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAssignmentResponse {
        private UUID id;
        private String lessonTitle;
        private String status;
        private java.time.LocalDateTime deadline;
    }
}
