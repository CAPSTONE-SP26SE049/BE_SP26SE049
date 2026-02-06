package com.aiservice.presentation.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
}
