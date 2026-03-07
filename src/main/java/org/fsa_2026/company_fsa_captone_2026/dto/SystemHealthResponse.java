package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Mock DTO for System Health
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemHealthResponse implements Serializable {
    private String status;
    private String databaseStatus;
    private String aiModelStatus;
    private long uptimeSeconds;
}
