package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomPerformanceResponse {
    private String classroomId;
    private String classroomName;
    private BigDecimal averageScore;
    private BigDecimal completionRate;
    private List<CommonErrorResponse> commonErrors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommonErrorResponse {
        private String phoneme;
        private Long occurrenceCount;
        private BigDecimal averageErrorScore;
    }
}
