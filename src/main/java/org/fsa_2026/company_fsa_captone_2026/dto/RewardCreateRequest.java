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
 * Điều kiện unlock: vượt qua quiz được gắn thành tựu này (không cần criteriaJson).
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

    private String iconUrl;

    /** FIX: Use Boolean (wrapper) instead of boolean (primitive) to allow null from FE */
    @JsonProperty("isActive")
    @Builder.Default
    private Boolean isActive = true;
}

