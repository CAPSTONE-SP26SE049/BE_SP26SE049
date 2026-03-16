package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;

import java.io.Serializable;
import java.util.Map;

/**
 * Badge Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeResponse implements Serializable {

    private String id;
    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private Map<String, Object> criteria;

    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseCriteria(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static BadgeResponse fromEntity(RewardCatalog badge) {
        if (badge == null)
            return null;
        return BadgeResponse.builder()
                .id(badge.getId().toString())
                .code(badge.getCode())
                .name(badge.getName())
                .description(badge.getDescription())
                .iconUrl(badge.getIconUrl())
                .criteria(parseCriteria(badge.getCriteriaJson()))
                .build();
    }
}
