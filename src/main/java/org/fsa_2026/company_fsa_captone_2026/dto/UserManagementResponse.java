package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;

import java.time.Instant;
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

    public static UserManagementResponse fromEntity(Account account) {
        return UserManagementResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .fullName(account.getUserProfile() != null ? account.getUserProfile().getFullName() : null)
                .roleCode(account.getRoleCode() != null ? account.getRoleCode().name() : null)
                .isActive(account.getIsActive())
                .emailVerified(account.getEmailVerified())
                .createdAt(account.getCreatedAt())
                .totalStars(account.getUserProfile() != null ? account.getUserProfile().getTotalStars() : 0)
                .currentStreakDays(
                        account.getUserProfile() != null ? account.getUserProfile().getCurrentStreakDays() : 0)
                .build();
    }
}
