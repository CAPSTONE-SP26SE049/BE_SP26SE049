package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for user search results in the friend system.
 * friendshipStatus is null if no relationship exists.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendSearchResponse {

    private UUID userId;
    private String fullName;
    private String avatarUrl;
    /** null = not connected, otherwise PENDING / ACCEPTED / BLOCKED */
    private String friendshipStatus;
}
