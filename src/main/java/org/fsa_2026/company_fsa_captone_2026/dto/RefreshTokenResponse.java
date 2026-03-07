package org.fsa_2026.company_fsa_captone_2026.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Refresh Token Response DTO
 * Returned after successful token refresh
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenResponse implements Serializable {

    private String accessToken;
    private String refreshToken;
}

