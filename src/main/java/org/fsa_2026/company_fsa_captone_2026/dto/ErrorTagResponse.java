package org.fsa_2026.company_fsa_captone_2026.dto;

import java.time.Instant;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorTagResponse implements Serializable {

    private String id;
    private String tagCode;
    private String name;
    private String description;
    private java.util.List<String> regions;
    private Instant createdAt;
    private Instant updatedAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static ErrorTagResponse fromEntity(LearningUnit tag) {
        if (tag == null)
            return null;

        String tagCode = "";
        String description = "";
        List<String> regions = new java.util.ArrayList<>();

        try {
            if (tag.getMetadataJson() != null) {
                Map<String, Object> metadata = objectMapper.readValue(tag.getMetadataJson(), Map.class);
                tagCode = (String) metadata.get("tag_code");
                description = (String) metadata.get("description");
                regions = (List<String>) metadata.get("regions");
            }
        } catch (JsonProcessingException | ClassCastException ignored) {
            // Keep fallback values when metadata is missing or malformed.
        }

        return ErrorTagResponse.builder()
                .id(tag.getId().toString())
                .tagCode(tagCode)
                .name(tag.getName())
                .description(description)
                .regions(regions)
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }
}