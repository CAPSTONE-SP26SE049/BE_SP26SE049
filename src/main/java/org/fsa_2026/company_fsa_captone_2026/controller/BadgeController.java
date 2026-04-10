package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.*;
import org.fsa_2026.company_fsa_captone_2026.service.BadgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Badge Controller — /api/v1/badges
 *
 * All badge-related APIs in one place:
 *
 * ── Public ────────────────────────────────────────────
 * GET /badges/catalog → Active badges list (for learner view)
 * GET /badges/my-badges → Authenticated user's unlocked badges
 *
 * ── Admin (requires ADMIN role) ──────────────────────
 * GET /badges → All badges (including hidden)
 * GET /badges/{id} → Badge detail by ID
 * POST /badges → Create new badge
 * PUT /badges/{id} → Update badge
 * PATCH /badges/{id}/toggle → Toggle visibility
 * DELETE /badges/{id} → Delete badge
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX)
@RequiredArgsConstructor
@Tag(name = "Badge", description = "Badge APIs — Public catalog, My Badges, and Admin CRUD")
public class BadgeController {

    private final BadgeService badgeService;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /badges/catalog — Danh sách huy hiệu đang hiển thị (cho learner)
     * Không cần đăng nhập.
     */
    @GetMapping("/public/badges/catalog")
    @Operation(summary = "Get Public Badge Catalog", description = "Returns all active badges for the learner view")
    public ResponseEntity<ApiResponse<List<BadgeResponse>>> getPublicCatalog() {
        log.info("GET /public/badges/catalog — public badge catalog");
        return ResponseEntity.ok(
                ApiResponse.success("Badges retrieved successfully", badgeService.getAllBadges()));
    }

    /**
     * GET /badges/my-badges — Huy hiệu đã mở khóa của tôi
     * Yêu cầu đăng nhập (bất kỳ role).
     */
    @GetMapping("/learner/my-badges")
    @Operation(summary = "Get My Badges", description = "Returns all badges unlocked by the currently authenticated user", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<AccountBadgeResponse>>> getMyBadges(Authentication authentication) {
        log.info("GET /learner/my-badges — user: {}", authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.success("My badges retrieved successfully",
                        badgeService.getMyBadges(authentication.getName())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN endpoints — yêu cầu role ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /badges — Toàn bộ huy hiệu (kể cả đang ẩn), dùng cho trang quản lý Admin
     * MIGRATED to AdminController.java to fix security routing issues
     */
    /*
     * @GetMapping("/admin/rewards")
     * 
     * @PreAuthorize("hasRole('ADMIN')")
     * 
     * @Operation(summary = "[Admin] Get All Badges",
     * description =
     * "Returns all badges including hidden ones — for Admin management page",
     * security = @SecurityRequirement(name = "bearer-jwt"))
     * public ResponseEntity<ApiResponse<List<RewardResponse>>> getAllForAdmin() {
     * log.info("[Admin] GET /badges — list all rewards");
     * return ResponseEntity.ok(
     * ApiResponse.success("All badges retrieved successfully",
     * adminService.getAllRewards()));
     * }
     */

}
