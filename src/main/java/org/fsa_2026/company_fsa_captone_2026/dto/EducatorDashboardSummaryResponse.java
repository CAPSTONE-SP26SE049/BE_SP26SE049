package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducatorDashboardSummaryResponse implements Serializable {

    private long totalStudents;
    private long activeClassrooms;
    private long totalAttempts;
    private double averageClassScore;
}
