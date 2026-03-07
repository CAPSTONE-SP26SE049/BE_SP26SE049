package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LeaderboardEntryResponse;
import org.fsa_2026.company_fsa_captone_2026.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/leaderboards")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Game Leaderboards for Players")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/global")
    @Operation(summary = "Get Global Leaderboard", description = "Get top 10 players globally by experience")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getGlobalLeaderboard() {
        log.info("Fetching global leaderboard");
        List<LeaderboardEntryResponse> board = leaderboardService.getGlobalLeaderboard();
        return ResponseEntity.ok(ApiResponse.success("Lấy bảng xếp hạng tổng thành công", board));
    }

    @GetMapping("/region/{regionCode}")
    @Operation(summary = "Get Regional Leaderboard", description = "Get top 10 players for a specific region (NORTH, CENTRAL, SOUTH)")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getRegionalLeaderboard(
            @PathVariable String regionCode) {
        log.info("Fetching regional leaderboard for: {}", regionCode);
        List<LeaderboardEntryResponse> board = leaderboardService.getRegionalLeaderboard(regionCode);
        return ResponseEntity.ok(ApiResponse.success("Lấy bảng xếp hạng khu vực thành công", board));
    }
}
