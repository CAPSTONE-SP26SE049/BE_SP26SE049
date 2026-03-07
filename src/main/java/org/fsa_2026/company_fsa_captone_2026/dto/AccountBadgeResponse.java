package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountBadge;

import java.io.Serializable;
import java.time.Instant;

/**
 * Account Badge Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBadgeResponse implements Serializable {

    private String accountId;
    private BadgeResponse badge;
    private Instant earnedAt;

    public static AccountBadgeResponse fromEntity(AccountBadge accountBadge) {
        if (accountBadge == null)
            return null;
        return AccountBadgeResponse.builder()
                .accountId(accountBadge.getAccount() != null ? accountBadge.getAccount().getId().toString() : null)
                .badge(BadgeResponse.fromEntity(accountBadge.getBadge()))
                .earnedAt(accountBadge.getEarnedAt())
                .build();
    }
}
