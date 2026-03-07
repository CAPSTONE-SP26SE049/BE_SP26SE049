package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Dialect;

import java.io.Serializable;

/**
 * Dialect Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DialectResponse implements Serializable {

    private String id;
    private String name;
    private String description;

    public static DialectResponse fromEntity(Dialect dialect) {
        if (dialect == null)
            return null;
        return DialectResponse.builder()
                .id(dialect.getId().toString())
                .name(dialect.getName())
                .description(dialect.getDescription())
                .build();
    }
}
