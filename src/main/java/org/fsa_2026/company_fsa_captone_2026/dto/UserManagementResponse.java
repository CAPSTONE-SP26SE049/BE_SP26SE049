package org.fsa_2026.company_fsa_captone_2026.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String roleCode;
    /** Luôn serialize đúng tên field cho FE (tránh Jackson đổi thành "active"). */
    @JsonProperty("isActive")
    private Boolean isActive;
    @JsonProperty("emailVerified")
    private Boolean emailVerified;
    private Instant createdAt;
    private Integer totalStars;
    private Integer currentStreakDays;
    /** Liên hệ & hồ sơ — dùng cho màn quản trị người dùng */
    private String phone;
    private String region;
    private String avatarUrl;
    private LocalDate lastLoginDate;
    private Integer totalExperience;

    public static UserManagementResponse fromEntity(Account account) {
        return UserManagementResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .roleCode(account.getRoleCode() != null ? account.getRoleCode().name() : null)
                .isActive(account.getIsActive())
                .emailVerified(account.getEmailVerified())
                .createdAt(account.getCreatedAt())
                .totalStars(account.getTotalStars() != null ? account.getTotalStars() : 0)
                .currentStreakDays(account.getCurrentStreakDays() != null ? account.getCurrentStreakDays() : 0)
                .phone(account.getPhone())
                .region(account.getRegion())
                .avatarUrl(account.getAvatarUrl())
                .lastLoginDate(account.getLastLoginDate())
                .totalExperience(account.getTotalExperience() != null ? account.getTotalExperience() : 0)
                .build();
    }
}
