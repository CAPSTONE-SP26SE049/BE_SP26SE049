package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Badge;

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

    public static BadgeResponse fromEntity(Badge badge) {
        if (badge == null)
            return null;
        return BadgeResponse.builder()
                .id(badge.getId().toString())
                .code(badge.getCode())
                .name(badge.getName())
                .description(badge.getDescription())
                .iconUrl(badge.getIconUrl())
                .criteria(badge.getCriteriaJson())
                .build();
    }
}
