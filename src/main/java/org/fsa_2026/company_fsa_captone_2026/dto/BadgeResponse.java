package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;

import java.io.Serializable;

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
    private String iconUrl;

    public static BadgeResponse fromEntity(RewardCatalog badge) {
        if (badge == null)
            return null;
        return BadgeResponse.builder()
                .id(badge.getId().toString())
                .code(badge.getCode())
                .name(badge.getName())
                .iconUrl(badge.getIconUrl())
                .build();
    }
}

