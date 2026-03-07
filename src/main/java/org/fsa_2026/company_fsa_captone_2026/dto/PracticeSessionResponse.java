package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.PracticeSession;

import java.io.Serializable;
import java.time.Instant;

/**
 * PracticeSession Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeSessionResponse implements Serializable {

    private String id;
    private String accountId;
    private Instant startedAt;
    private Instant endedAt;

    public static PracticeSessionResponse fromEntity(PracticeSession session) {
        if (session == null)
            return null;
        return PracticeSessionResponse.builder()
                .id(session.getId().toString())
                .accountId(session.getAccount() != null ? session.getAccount().getId().toString() : null)
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();
    }
}
