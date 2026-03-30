package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * RewardResponse — DTO trả về thông tin của một huy hiệu (dùng cho Admin)
 * Bao gồm thông tin quiz/level đang liên kết (nếu có).
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
    private String iconUrl;

    /**
     * FIX: Lombok boolean `isActive` → getter `isActive()` → Jackson serializes as `active`.
     * @JsonProperty forces JSON key to be `isActive` so FE receives correct field.
     */
    @JsonProperty("isActive")
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    /** Quiz đang liên kết với thành tựu này (null nếu chưa gắn) */
    private UUID linkedQuizId;
    private String linkedQuizName;
    /** Level chứa quiz đang liên kết */
    private String linkedLevelName;

    public static RewardResponse fromEntity(RewardCatalog entity) {
        if (entity == null) return null;
        return RewardResponse.builder()
                .id(entity.getId().toString())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .iconUrl(entity.getIconUrl())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

