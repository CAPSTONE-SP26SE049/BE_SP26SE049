package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Mock DTO for Analytics Overview
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsOverviewResponse implements Serializable {
    private long totalUsers;
    private long activeUsers7Days;
    private long totalAttempts;
    private double averageScore;
}
