package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserProfileResponse;
import org.fsa_2026.company_fsa_captone_2026.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.fsa_2026.company_fsa_captone_2026.service.UserProfileService;

/**
 * User Controller
 * Handles user profile operations
 * Base: /api/v1/users
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User API endpoints")
public class UserController {

    private final AuthService authService;
    private final UserProfileService userProfileService;

    /**
     * Get Current User Profile - GET /api/v1/users/me
     * Protected: Requires Authorization: Bearer <token>
     */
    @GetMapping("/me")
    @Operation(summary = "Get User Profile", description = "Retrieve the currently authenticated user's profile", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile(Authentication authentication) {
        log.info("Get profile for user: {}", authentication.getName());

        UserProfileResponse profile = authService.getUserProfile(authentication.getName());

        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cá nhân thành công", profile));
    }

    /**
     * Update Current User Profile - PUT /api/v1/users/me
     * Protected: Requires Authorization: Bearer <token>
     */
    @PutMapping("/me")
    @Operation(summary = "Update User Profile", description = "Update the currently authenticated user's profile information", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUserProfile(
            Authentication authentication,
            @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.UserProfileRequest request) {

        log.info("Update profile for user: {}", authentication.getName());

        UserProfileResponse updatedProfile = userProfileService.updateProfile(authentication.getName(), request);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", updatedProfile));
    }
}
