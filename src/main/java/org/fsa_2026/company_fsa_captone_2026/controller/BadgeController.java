package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.AccountBadgeResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.BadgeResponse;
import org.fsa_2026.company_fsa_captone_2026.service.BadgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/badges")
@RequiredArgsConstructor
@Tag(name = "Badge/Achievement", description = "Game Achievement APIs")
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    @Operation(summary = "Get All Badges", description = "List all possible badges in the game")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getAllBadges() {
        log.info("Get all possible badges");
        List<BadgeResponse> badges = badgeService.getAllBadges();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách huy hiệu thành công", badges));
    }

    @GetMapping("/my-badges")
    @Operation(summary = "Get My Badges", description = "Get badges earned by the currently authenticated user", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<AccountBadgeResponse>>> getMyBadges(Authentication authentication) {
        log.info("Get earned badges for user: {}", authentication.getName());
        List<AccountBadgeResponse> myBadges = badgeService.getMyBadges(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách huy hiệu đã đạt được thành công", myBadges));
    }
}
