package org.fsa_2026.company_fsa_captone_2026.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a friendship record.
 * Shows the "other" user's info relative to the current user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendshipResponse {

    private UUID friendshipId;
    private UUID userId;
    private String fullName;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    private String status;
    private Instant createdAt;
}
