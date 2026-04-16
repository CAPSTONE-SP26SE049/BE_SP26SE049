package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnalyticsResponse {
    private UUID id;
    private String fullName;
    private String email;
    private Integer completedQuizzes;
    private Integer totalQuizzes;
    private Integer totalStars;
    private Integer currentStreak;
    private Double averageScore;
}
