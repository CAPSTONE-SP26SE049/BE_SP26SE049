package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryTestResultResponse {
    private UUID id;
    private Double overallScore;
    private String detectedRegion;
    private Integer totalQuestions;
    private Instant createdAt;
    private String userEmail;
    private String userFullName;
    private String details;
}
