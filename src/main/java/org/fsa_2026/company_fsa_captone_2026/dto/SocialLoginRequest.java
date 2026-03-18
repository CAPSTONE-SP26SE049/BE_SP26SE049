package org.fsa_2026.company_fsa_captone_2026.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Social Login Request DTO
 * Used for Google / Facebook OAuth2 login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {

    @NotBlank(message = "Provider is required (GOOGLE / FACEBOOK)")
    private String provider;

    @NotBlank(message = "Token is required")
    private String token;
}
