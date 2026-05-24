package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.service.TournamentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/tournaments")
@RequiredArgsConstructor
@Tag(name = "Admin Tournament API", description = "Quản lý và chốt các giải đấu tuần")
public class TournamentController {

    private final TournamentService tournamentService;

    @PostMapping("/finalize")
    @Operation(summary = "Chốt giải đấu tuần", description = "Chốt giải đấu tuần đang diễn ra (hoặc theo ID cụ thể), trao thưởng XP/Badge cho Top 3 người dẫn đầu, tự động mở giải đấu mới.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> finalizeWeeklyTournament(
            @RequestParam(value = "tournamentId", required = false) UUID tournamentId) {
        log.info("Admin finalising tournament: {}", tournamentId);
        Map<String, Object> report = tournamentService.finalizeWeeklyTournament(tournamentId);
        return ResponseEntity.ok(ApiResponse.success("Chốt giải đấu thành công", report));
    }
}
