package org.fsa_2026.company_fsa_captone_2026.dto;

import java.io.Serializable;
import java.util.Map;

import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @SuppressWarnings("unchecked")
    public static DialectResponse fromEntity(LearningUnit dialect) {
        if (dialect == null)
            return null;
        
        String description = "";
        try {
            if (dialect.getMetadataJson() != null) {
                Map<String, Object> metadata = objectMapper.readValue(dialect.getMetadataJson(), Map.class);
                description = (String) metadata.get("description");
            }
        } catch (JsonProcessingException | ClassCastException ignored) {
            // Keep empty description when metadata parsing fails.
        }

        return DialectResponse.builder()
                .id(dialect.getId().toString())
                .name(dialect.getName())
                .description(description)
                .build();
    }
}