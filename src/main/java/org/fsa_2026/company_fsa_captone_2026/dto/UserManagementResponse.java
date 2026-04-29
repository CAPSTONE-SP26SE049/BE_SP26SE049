package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;

import java.time.Instant;
import java.time.LocalDateTime;
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
    private Boolean isActive;
    private Boolean emailVerified;
    private Instant createdAt;
    private Integer totalStars;
    private Integer currentStreakDays;
    private LocalDateTime lastActiveAt;
    private Long unreadCount;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private String avatarUrl;
    private String level;
    private Integer progressPercent;
    private Integer pronunciationScore;
    private Boolean hasCustomPath;
    private String customPathType;

    public static UserManagementResponse fromEntity(Account account) {
        return fromEntity(account, false, "NONE");
    }

    public static UserManagementResponse fromEntity(Account account, boolean hasCustomPath, String customPathType) {
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
                .hasCustomPath(hasCustomPath)
                .customPathType(customPathType != null ? customPathType : "NONE")
                .avatarUrl(account.getAvatarUrl())
                .level(account.getRegion() != null ? account.getRegion() : "N/A")
                .progressPercent(
                        Math.min(100, (account.getTotalExperience() != null ? account.getTotalExperience() : 0) / 10))
                .pronunciationScore(Math.min(100, 60 + (account.getTotalStars() == null ? 0 : account.getTotalStars())))
                .build();
    }
}
