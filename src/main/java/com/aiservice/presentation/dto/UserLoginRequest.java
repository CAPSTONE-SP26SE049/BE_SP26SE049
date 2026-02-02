package com.aiservice.presentation.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginRequest {
    private String username;
    private String password;
}
