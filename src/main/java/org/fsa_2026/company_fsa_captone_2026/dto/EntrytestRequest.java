package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EntrytestRequest DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrytestRequest {
    private String audioUrl; // URL of the recorded audio file
}
