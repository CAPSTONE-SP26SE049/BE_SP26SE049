package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.service.DailyChallengeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/daily-challenges")
@RequiredArgsConstructor
@Tag(name = "Daily Challenge API", description = "Quản lý thử thách phát âm hàng ngày")
public class DailyChallengeController {

    private final DailyChallengeService dailyChallengeService;

    @GetMapping
    @Operation(summary = "Lấy thử thách hàng ngày", description = "Lấy 3 thử thách phát âm của hôm nay")
    public ResponseEntity<ApiResponse<List<ChallengeBank>>> getDailyChallenges(Authentication authentication) {
        log.info("Request daily challenges");
        String email = authentication != null ? authentication.getName() : null;
        List<ChallengeBank> challenges = dailyChallengeService.getDailyChallenges(email);
        return ResponseEntity.ok(ApiResponse.success("Thành công", challenges));
    }

    /**
     * POST /api/v1/daily-challenges/{challengeId}/submit — Nộp ghi âm .webm (spec Gameplay).
     * Xác thực 401 do SecurityFilter; không trả 417 thủ công nữa.
     */
    @PostMapping("/{challengeId}/submit")
    @Operation(summary = "Nộp bài làm thử thách hàng ngày", description = "Chấm phát âm từ file .webm, lưu session và tặng bonus XP khi hoàn thành đủ 3 câu", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitDailyChallenge(
            @PathVariable("challengeId") UUID challengeId,
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "dialect", defaultValue = "BAC") String dialect,
            Authentication authentication) throws IOException {
        log.info("User {} submitting daily challenge {}", authentication.getName(), challengeId);
        Map<String, Object> result = dailyChallengeService.submitDailyChallenge(
                authentication.getName(), challengeId, audio, dialect);
        return ResponseEntity.ok(ApiResponse.success("Nộp bài và đánh giá thành công", result));
    }
}
