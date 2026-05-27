package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LeaderboardEntryResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LeaderboardResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardPeriodType;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardScope;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.LeaderboardSortBy;
import org.fsa_2026.company_fsa_captone_2026.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/leaderboards")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Regional & Global Leaderboards for Players")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    // ─── Global Leaderboard ──────────────────────────────────────────────

    @GetMapping("/global")
    @Operation(
            summary = "Get Global Leaderboard",
            description = "Get top 50 players globally. Supports filtering by period (DAILY/WEEKLY/MONTHLY/ALL_TIME) "
                    + "and sort criteria (TOTAL_XP/TOTAL_STARS/CHALLENGES_COMPLETED). "
                    + "If authenticated, the response includes the user's own rank.")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getGlobalLeaderboard(
            @RequestParam(defaultValue = "ALL_TIME") LeaderboardPeriodType period,
            @RequestParam(defaultValue = "TOTAL_STARS") LeaderboardSortBy sortBy,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        log.info("Fetching global leaderboard: period={}, sortBy={}, user={}", period, sortBy, userEmail);

        LeaderboardResponse board = leaderboardService.getGlobalLeaderboard(period, sortBy, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy bảng xếp hạng toàn cầu thành công", board));
    }

    // ─── Regional Leaderboard ────────────────────────────────────────────

    @GetMapping("/region/{regionCode}")
    @Operation(
            summary = "Get Regional Leaderboard",
            description = "Get top 50 players for a specific region (NORTH/CENTRAL/SOUTH). "
                    + "Supports filtering by period and sort criteria. "
                    + "If authenticated, the response includes the user's own rank.")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getRegionalLeaderboard(
            @PathVariable String regionCode,
            @RequestParam(defaultValue = "ALL_TIME") LeaderboardPeriodType period,
            @RequestParam(defaultValue = "TOTAL_STARS") LeaderboardSortBy sortBy,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        log.info("Fetching regional leaderboard: region={}, period={}, sortBy={}, user={}",
                regionCode, period, sortBy, userEmail);

        LeaderboardResponse board = leaderboardService.getRegionalLeaderboard(regionCode, period, sortBy, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy bảng xếp hạng khu vực thành công", board));
    }

    // ─── My Rank ─────────────────────────────────────────────────────────

    /**
     * GET /api/v1/leaderboards/my-rank — Fix U-03/U-04 tại LeaderboardService (period, sortBy, region bắt buộc).
     */
    @GetMapping("/my-rank")
    @Operation(
            summary = "Get My Rank",
            description = "Get the authenticated user's rank position in a specific leaderboard. "
                    + "Requires authentication. scope=REGIONAL requires region query param.",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<LeaderboardEntryResponse>> getMyRank(
            @RequestParam(defaultValue = "GLOBAL") LeaderboardScope scope,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "ALL_TIME") LeaderboardPeriodType period,
            @RequestParam(defaultValue = "TOTAL_STARS") LeaderboardSortBy sortBy,
            Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("Fetching my rank: scope={}, region={}, period={}, sortBy={}, user={}",
                scope, region, period, sortBy, userEmail);

        LeaderboardEntryResponse myRank = leaderboardService.getMyRank(scope, region, period, sortBy, userEmail);
        if (myRank == null) {
            return ResponseEntity.ok(ApiResponse.success("Bạn chưa có thứ hạng trong bảng xếp hạng này", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy thứ hạng thành công", myRank));
    }

    // ─── Manual Refresh ─────────────────────────────────────────────────

    @PostMapping("/refresh")
    @Operation(summary = "Manual Refresh Leaderboards", description = "Force all leaderboard snapshots to update immediately.")
    public ResponseEntity<ApiResponse<Void>> refreshLeaderboards() {
        log.info("Manual leaderboard refresh requested");
        leaderboardService.refreshAllLeaderboards();
        return ResponseEntity.ok(ApiResponse.success("Đã làm mới bảng xếp hạng", null));
    }
}
