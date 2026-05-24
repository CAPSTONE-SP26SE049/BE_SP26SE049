package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<ApiResponse<List<ChallengeBank>>> getDailyChallenges() {
        log.info("Request daily challenges");
        List<ChallengeBank> challenges = dailyChallengeService.getDailyChallenges();
        return ResponseEntity.ok(ApiResponse.success("Thành công", challenges));
    }

    @PostMapping("/submit")
    @Operation(summary = "Nộp bài làm thử thách hàng ngày", description = "Chấm điểm phát âm bằng âm thanh ghi âm, upload lên Firebase, lưu vết và tặng bonus XP nếu hoàn thành đủ 3 câu")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitDailyChallenge(
            @RequestParam("challengeId") UUID challengeId,
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "dialect", defaultValue = "BAC") String dialect,
            Authentication authentication) throws IOException {
        
        if (authentication == null) {
            return ResponseEntity.status(417).body(ApiResponse.error("Chưa xác thực người dùng"));
        }
        
        log.info("User {} submitting daily challenge {}", authentication.getName(), challengeId);
        Map<String, Object> result = dailyChallengeService.submitDailyChallenge(
                authentication.getName(), challengeId, audio, dialect);
        return ResponseEntity.ok(ApiResponse.success("Nộp bài và đánh giá thành công", result));
    }
}
