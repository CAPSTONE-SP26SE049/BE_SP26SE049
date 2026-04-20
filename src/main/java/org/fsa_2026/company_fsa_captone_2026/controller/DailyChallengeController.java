package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.DailyChallengeResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.DailyChallengeSubmissionRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.DailyChallengeSubmissionResponse;
import org.fsa_2026.company_fsa_captone_2026.service.DailyChallengeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/daily-challenge")
@RequiredArgsConstructor
@Tag(name = "Daily Challenge", description = "Endpoints cho tính năng thử thách hàng ngày")
public class DailyChallengeController {

    private final DailyChallengeService dailyChallengeService;

    @GetMapping
    @Operation(summary = "Lấy thử thách hàng ngày hiện tại")
    public ResponseEntity<DailyChallengeResponse> getCurrentChallenge() {
        return ResponseEntity.ok(dailyChallengeService.getCurrentDailyChallenge());
    }

    @PostMapping("/submit")
    @Operation(summary = "Gửi đáp án cho thử thách hàng ngày")
    public ResponseEntity<DailyChallengeSubmissionResponse> submitSolution(
            @RequestBody DailyChallengeSubmissionRequest request) {
        return ResponseEntity.ok(dailyChallengeService.submitSolution(request));
    }
}
