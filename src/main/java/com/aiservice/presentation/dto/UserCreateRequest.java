package com.aiservice.presentation.dto;

import com.aiservice.domain.entities.User;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid format")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
        message = "Username can only contain letters, numbers, underscore, and hyphen")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]*$",
        message = "Password must contain letters, numbers, and special characters"
    )
    private String password;

    private User.UserRole role;
}
