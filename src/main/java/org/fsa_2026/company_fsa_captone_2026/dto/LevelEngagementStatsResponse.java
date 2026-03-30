package org.fsa_2026.company_fsa_captone_2026.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thống kê tham gia / đạt quiz theo học phần (level), gộp từ account_learning_unit trên các quiz con.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelEngagementStatsResponse implements Serializable {

    private String levelId;
    private long learnerCount;
    private int successRate;
    private int failRate;
}
