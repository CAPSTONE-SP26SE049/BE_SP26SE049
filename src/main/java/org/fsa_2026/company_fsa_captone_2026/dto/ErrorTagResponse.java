package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ErrorTagResponse fromEntity(LearningUnit tag) {
        if (tag == null)
            return null;

        String tagCode = "";
        String description = "";
        List<String> regions = null;

        try {
            if (tag.getMetadataJson() != null) {
                Map<String, Object> metadata = objectMapper.readValue(tag.getMetadataJson(), Map.class);
                tagCode = (String) metadata.get("tag_code");
                description = (String) metadata.get("description");
                regions = (List<String>) metadata.get("regions");
            }
        } catch (Exception e) {
            // Log
        }

        return ErrorTagResponse.builder()
                .id(tag.getId().toString())
                .tagCode(tagCode)
                .name(tag.getName())
                .description(description)
                .regions(regions)
                .build();
    }
}