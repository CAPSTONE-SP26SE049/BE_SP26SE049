package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Tournament;
import org.fsa_2026.company_fsa_captone_2026.service.TournamentService;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
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

    @PutMapping("/active")
    @Operation(summary = "Cập nhật thông tin giải đấu đang hoạt động", description = "Chỉnh sửa tên, mô tả và thời gian kết thúc của giải đấu tuần hiện tại.")
    public ResponseEntity<ApiResponse<Tournament>> updateActiveTournament(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "endsAt", required = false) String endsAtStr) {
        log.info("Admin updating active tournament. Name: {}, Description: {}, EndsAt: {}", name, description, endsAtStr);
        Instant endsAt = null;
        if (endsAtStr != null && !endsAtStr.isBlank()) {
            try {
                endsAt = Instant.parse(endsAtStr);
            } catch (Exception e) {
                throw new ApiException("BAD_REQUEST", "Định dạng thời gian endsAt không hợp lệ (phải là ISO UTC e.g. 2026-06-02T15:53:51Z).");
            }
        }

        Tournament updated = tournamentService.updateActiveTournament(name, description, endsAt);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin giải đấu thành công", updated));
    }

    @PostMapping("/active/questions")
    @Operation(summary = "Gán thủ công danh sách câu hỏi cho giải đấu đang hoạt động", description = "Chọn thủ công danh sách câu hỏi phát âm từ ChallengeBank để làm câu hỏi thi đấu tuần này.")
    public ResponseEntity<ApiResponse<Tournament>> assignActiveTournamentQuestions(
            @RequestBody List<UUID> questionIds) {
        log.info("Admin assigning {} questions manually to active tournament", questionIds != null ? questionIds.size() : 0);
        Tournament updated = tournamentService.assignActiveTournamentQuestions(questionIds);
        return ResponseEntity.ok(ApiResponse.success("Gán câu hỏi giải đấu thành công", updated));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả giải đấu", description = "Lấy tất cả các giải đấu bao gồm Đang diễn ra, Đã kết thúc và Đang chuẩn bị.")
    public ResponseEntity<ApiResponse<List<Tournament>>> getAllTournaments() {
        log.info("Admin fetching all tournaments");
        List<Tournament> list = tournamentService.getAllTournaments();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách giải đấu thành công", list));
    }

    @PostMapping
    @Operation(summary = "Tạo giải đấu chuẩn bị diễn ra (UPCOMING)", description = "Lên lịch trước giải đấu cho tuần sau hoặc tương lai, có thể chọn trước câu hỏi phát âm.")
    public ResponseEntity<ApiResponse<Tournament>> createUpcomingTournament(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "startsAt") String startsAtStr,
            @RequestParam(value = "endsAt") String endsAtStr,
            @RequestBody(required = false) List<UUID> questionIds) {
        log.info("Admin creating upcoming tournament. StartsAt: {}, EndsAt: {}", startsAtStr, endsAtStr);
        Instant startsAt;
        Instant endsAt;
        try {
            startsAt = Instant.parse(startsAtStr);
            endsAt = Instant.parse(endsAtStr);
        } catch (Exception e) {
            throw new ApiException("BAD_REQUEST", "Định dạng thời gian startsAt/endsAt không hợp lệ (phải là ISO UTC e.g. 2026-06-02T15:53:51Z).");
        }

        Tournament created = tournamentService.createUpcomingTournament(name, description, startsAt, endsAt, questionIds);
        return ResponseEntity.ok(ApiResponse.success("Tạo giải đấu chuẩn bị thành công", created));
    }

    @GetMapping("/{id}/leaderboard")
    @Operation(summary = "Lấy bảng xếp hạng của một giải đấu bất kỳ", description = "Trả về bảng xếp hạng chi tiết của giải đấu theo ID.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTournamentLeaderboard(
            @PathVariable("id") UUID id) {
        log.info("Admin fetching leaderboard for tournament: {}", id);
        List<Map<String, Object>> leaderboard = tournamentService.getTournamentLeaderboard(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy bảng xếp hạng giải đấu thành công", leaderboard));
    }
}

