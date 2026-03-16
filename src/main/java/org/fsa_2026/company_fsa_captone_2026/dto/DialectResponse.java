package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.Map;

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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static DialectResponse fromEntity(LearningUnit dialect) {
        if (dialect == null)
            return null;
        
        String description = "";
        try {
            if (dialect.getMetadataJson() != null) {
                Map<String, Object> metadata = objectMapper.readValue(dialect.getMetadataJson(), Map.class);
                description = (String) metadata.get("description");
            }
        } catch (Exception e) {
            // Log or ignore
        }

        return DialectResponse.builder()
                .id(dialect.getId().toString())
                .name(dialect.getName())
                .description(description)
                .build();
    }
}