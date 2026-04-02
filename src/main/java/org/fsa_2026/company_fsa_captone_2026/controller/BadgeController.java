package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.*;
import org.fsa_2026.company_fsa_captone_2026.service.AdminService;
import org.fsa_2026.company_fsa_captone_2026.service.BadgeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Badge Controller — /api/v1/badges
 *
 * All badge-related APIs in one place:
 *
 * ── Public ────────────────────────────────────────────
 *   GET  /badges/catalog        → Active badges list (for learner view)
 *   GET  /badges/my-badges      → Authenticated user's unlocked badges
 *
 * ── Admin (requires ADMIN role) ──────────────────────
 *   GET    /badges              → All badges (including hidden)
 *   GET    /badges/{id}         → Badge detail by ID
 *   POST   /badges              → Create new badge
 *   PUT    /badges/{id}         → Update badge
 *   PATCH  /badges/{id}/toggle  → Toggle visibility
 *   DELETE /badges/{id}         → Delete badge
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX)
@RequiredArgsConstructor
@Tag(name = "Badge", description = "Badge APIs — Public catalog, My Badges, and Admin CRUD")
public class BadgeController {

    private final BadgeService badgeService;
    private final AdminService adminService;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /badges/catalog — Danh sách huy hiệu đang hiển thị (cho learner)
     * Không cần đăng nhập.
     */
    @GetMapping("/public/badges/catalog")
    @Operation(summary = "Get Public Badge Catalog",
               description = "Returns all active badges for the learner view")
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
    @Operation(summary = "Get My Badges",
               description = "Returns all badges unlocked by the currently authenticated user",
               security = @SecurityRequirement(name = "bearer-jwt"))
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
     */
    @GetMapping("/admin/rewards")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get All Badges",
               description = "Returns all badges including hidden ones — for Admin management page",
               security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<RewardResponse>>> getAllForAdmin() {
        log.info("[Admin] GET /badges — list all rewards");
        return ResponseEntity.ok(
                ApiResponse.success("All badges retrieved successfully", adminService.getAllRewards()));
    }

    /**
     * GET /badges/{id} — Chi tiết một huy hiệu (admin)
     */
    @GetMapping("/admin/rewards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get Badge by ID",
               description = "Returns detailed information of a badge by its ID",
               security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<RewardResponse>> getById(@PathVariable UUID id) {
        log.info("[Admin] GET /badges/{} — detail", id);
        return ResponseEntity.ok(
                ApiResponse.success("Badge retrieved successfully", adminService.getRewardById(id)));
    }

    /**
     * POST /badges — Tạo huy hiệu mới (admin)
     */
    @PostMapping("/admin/rewards")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Create Badge",
               description = "Creates a new badge with the specified category, criteria, and unlock conditions",
               security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<RewardResponse>> create(
            @Valid @RequestBody RewardCreateRequest request) {
        log.info("[Admin] POST /badges — create: {}", request.getCode());
        RewardResponse created = adminService.createReward(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Badge created successfully", created));
    }

    /**
     * PUT /badges/{id} — Cập nhật huy hiệu (admin)
     */
    @PutMapping("/admin/rewards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Update Badge",
               description = "Updates an existing badge by its ID",
               security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<RewardResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RewardCreateRequest request) {
        log.info("[Admin] PUT /badges/{} — update", id);
        return ResponseEntity.ok(
                ApiResponse.success("Badge updated successfully", adminService.updateReward(id, request)));
    }

    /**
     * PATCH /badges/{id}/toggle — Bật/Tắt hiển thị huy hiệu (admin)
     */
    @PatchMapping("/admin/rewards/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Toggle Badge Visibility",
               description = "Toggles the badge active status (hidden ↔ visible to players)",
               security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<RewardResponse>> toggle(@PathVariable UUID id) {
        log.info("[Admin] PATCH /badges/{}/toggle", id);
        return ResponseEntity.ok(
                ApiResponse.success("Badge visibility updated", adminService.toggleRewardActive(id)));
    }

    /**
     * DELETE /badges/{id} — Xóa huy hiệu vĩnh viễn (admin)
     */
    @DeleteMapping("/admin/rewards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Delete Badge",
               description = "Permanently deletes a badge. Prefer using toggle to hide instead of deleting.",
               security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        log.info("[Admin] DELETE /badges/{}", id);
        adminService.deleteReward(id);
        return ResponseEntity.ok(ApiResponse.success("Badge deleted successfully", null));
    }
}
