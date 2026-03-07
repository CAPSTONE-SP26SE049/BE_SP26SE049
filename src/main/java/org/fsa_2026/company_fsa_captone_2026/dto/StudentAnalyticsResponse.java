package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnalyticsResponse implements Serializable {

    private UUID studentId;
    private String fullName;
    private List<ErrorMetric> topErrors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorMetric implements Serializable {
        private String phoneme;
        private double accuracy;
        private long count;
    }
}
