package com.aiservice.presentation.dto;

import com.aiservice.domain.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private User.UserRole role;
    private boolean isActive;
    private LocalDateTime createdAt;
}

