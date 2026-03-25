package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;

import java.io.Serializable;
import java.time.Instant;

/**
 * RewardResponse — DTO trả về thông tin của một huy hiệu (dùng cho Admin)
 * Không còn xpReward và rewardType — mọi phần thưởng đều là Huy hiệu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardResponse implements Serializable {

    private String id;
    private String code;
    private String name;
    private String description;
    private String category;
    private String iconUrl;
    private String criteriaJson;

    /**
     * FIX: Lombok boolean `isActive` → getter `isActive()` → Jackson serializes as `active`.
     * @JsonProperty forces JSON key to be `isActive` so FE receives correct field.
     */
    @JsonProperty("isActive")
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public static RewardResponse fromEntity(RewardCatalog entity) {
        if (entity == null) return null;
        return RewardResponse.builder()
                .id(entity.getId().toString())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .iconUrl(entity.getIconUrl())
                .criteriaJson(entity.getCriteriaJson())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
