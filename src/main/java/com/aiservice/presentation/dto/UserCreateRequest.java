package com.aiservice.presentation.dto;

import com.aiservice.domain.entities.User;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
    private String email;
    private String username;
    private String password;
    private User.UserRole role;
}
