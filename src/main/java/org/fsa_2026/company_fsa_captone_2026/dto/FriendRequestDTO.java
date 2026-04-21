package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * DTO for sending a friend request.
 */
@Data
public class FriendRequestDTO {

    @NotNull(message = "addresseeId không được để trống")
    private UUID addresseeId;
}
