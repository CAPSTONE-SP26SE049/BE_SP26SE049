package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.service.TournamentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/tournaments")
@RequiredArgsConstructor
@Tag(name = "Weekly Tournaments", description = "Các API phục vụ thi đấu Giải đấu tuần của Học viên")
public class LearnerTournamentController {

    private final TournamentService tournamentService;

    @GetMapping("/active")
    @Operation(
            summary = "Lấy giải đấu tuần đang hoạt động",
            description = "Trả về thông tin chi tiết giải đấu tuần đang kích hoạt, bộ 5 câu hỏi tuần này và tiến trình hiện tại của học viên đăng nhập.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveTournament(Authentication authentication) {
        String email = authentication.getName();
        log.info("Fetching active tournament details for user: {}", email);
        Map<String, Object> details = tournamentService.getActiveTournamentDetails(email);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giải tuần thành công", details));
    }

    @GetMapping("/active/leaderboard")
    @Operation(
            summary = "Lấy bảng xếp hạng giải đấu tuần",
            description = "Trả về danh sách những người tham gia có tổng điểm cao nhất trong giải tuần hiện tại để vinh danh.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveLeaderboard() {
        log.info("Fetching active weekly tournament leaderboard");
        List<Map<String, Object>> leaderboard = tournamentService.getActiveTournamentLeaderboard();
        return ResponseEntity.ok(ApiResponse.success("Lấy bảng xếp hạng giải tuần thành công", leaderboard));
    }

    @PostMapping("/submit")
    @Operation(
            summary = "Nộp điểm luyện tập câu hỏi giải đấu",
            description = "Học viên sau khi hoàn thành phát âm một câu hỏi thuộc giải đấu sẽ nộp điểm để ghi nhận lên hệ thống.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitTournamentScore(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        String email = authentication.getName();
        
        String challengeIdStr = (String) body.get("challengeId");
        if (challengeIdStr == null || challengeIdStr.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu challengeId"));
        }
        
        Object scoreObj = body.get("score");
        if (scoreObj == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu score"));
        }

        UUID challengeId = UUID.fromString(challengeIdStr);
        int score = ((Number) scoreObj).intValue();

        log.info("User {} submitting score {} for weekly tournament challenge {}", email, score, challengeId);
        Map<String, Object> result = tournamentService.submitTournamentScore(email, challengeId, score);
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận điểm số thành công", result));
    }

    @GetMapping("/history")
    @Operation(
            summary = "Lấy lịch sử các giải đấu tuần đã qua",
            description = "Trả về danh sách các giải đấu tuần đã kết thúc (FINISHED) kèm thông tin Quán quân, Á quân và Hạng 3 của mỗi giải.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFinishedTournamentsHistory() {
        log.info("Fetching finished weekly tournaments history");
        List<Map<String, Object>> history = tournamentService.getFinishedTournamentsHistory();
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử giải tuần thành công", history));
    }
}

