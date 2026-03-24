package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * RewardCreateRequest — DTO để Admin tạo/sửa huy hiệu
 * reward_type và xp_reward không còn cần thiết, mọi phần thưởng đều là HUY HIỆU
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardCreateRequest implements Serializable {

    @NotBlank(message = "Mã code không được để trống")
    private String code;

    @NotBlank(message = "Tên huy hiệu không được để trống")
    private String name;

    private String description;

    /** LEARNING | PRONUNCIATION | MINI_GAMES | SCORE | SOCIAL | SPECIAL | GENERAL */
    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    private String iconUrl;

    /**
     * JSON string định nghĩa điều kiện unlock.
     * Ví dụ: {"type":"levels_completed","threshold":5}
     */
    @NotBlank(message = "Tiêu chí unlock (criteriaJson) không được để trống")
    private String criteriaJson;

    /** FIX: @JsonProperty ensures FE sends 'isActive' and backend receives it correctly */
    @JsonProperty("isActive")
    @Builder.Default
    private boolean isActive = true;
}
