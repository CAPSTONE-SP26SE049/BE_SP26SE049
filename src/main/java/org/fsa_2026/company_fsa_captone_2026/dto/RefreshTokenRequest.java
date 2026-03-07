package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Refresh Token Request DTO
 * POST /api/v1/auth/refresh
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest implements Serializable {

    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}

