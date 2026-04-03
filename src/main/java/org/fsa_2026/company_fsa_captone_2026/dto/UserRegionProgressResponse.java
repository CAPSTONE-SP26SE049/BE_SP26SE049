package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegionProgressResponse implements Serializable {
    private List<RegionProgress> regions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegionProgress implements Serializable {
        private String regionName;
        private int totalStars;
        private int totalQuizzes;
        private int completedQuizzes;
        private double completionPercentage;
    }
}
