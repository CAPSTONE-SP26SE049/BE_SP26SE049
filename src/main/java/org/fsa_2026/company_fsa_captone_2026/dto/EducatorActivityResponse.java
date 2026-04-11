package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducatorActivityResponse {
    private String title;
    private String timeDescription; // e.g. "2 minutes ago"
    private Instant createdAt;
    private String type; // e.g. "COMPLETED", "FEEDBACK", "SYSTEM"
    private String studentName;
}
