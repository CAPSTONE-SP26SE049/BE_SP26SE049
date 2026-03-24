package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * EntrytestResponse DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrytestResponse {
    private String suggestedRegion; // "BAC", "TRUNG", "NAM"
    private BigDecimal confidence;    // AI confidence score (0-1)
    private String feedback;        // Text feedback
}
