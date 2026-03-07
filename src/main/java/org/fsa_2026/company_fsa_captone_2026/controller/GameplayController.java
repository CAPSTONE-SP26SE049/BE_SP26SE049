package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.AttemptRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.AttemptResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.PracticeSessionResponse;
import org.fsa_2026.company_fsa_captone_2026.service.GameplayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/gameplay")
@RequiredArgsConstructor
@Tag(name = "Gameplay", description = "Game Core Loop APIs (Sessions, Attempts)")
public class GameplayController {

    private final GameplayService gameplayService;

    @PostMapping("/sessions")
    @Operation(summary = "Start Practice Session", description = "Start a new gameplay practice session", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<PracticeSessionResponse>> startSession(Authentication authentication) {
        log.info("Starting session for user: {}", authentication.getName());
        PracticeSessionResponse session = gameplayService.startSession(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Bắt đầu phiên học thành công", session));
    }

    @PutMapping("/sessions/{sessionId}/end")
    @Operation(summary = "End Practice Session", description = "End an active gameplay practice session", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<PracticeSessionResponse>> endSession(
            @PathVariable String sessionId,
            Authentication authentication) {
        log.info("Ending session {} for user: {}", sessionId, authentication.getName());
        PracticeSessionResponse session = gameplayService.endSession(authentication.getName(), sessionId);
        return ResponseEntity.ok(ApiResponse.success("Kết thúc phiên học thành công", session));
    }

    @PostMapping("/attempts")
    @Operation(summary = "Submit Pronunciation Attempt", description = "Submit audio for a challenge and receive AI-based scoring feedback", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<AttemptResponse>> submitAttempt(
            @RequestBody AttemptRequest request,
            Authentication authentication) {
        log.info("Evaluating attempt for user: {}", authentication.getName());
        AttemptResponse attempt = gameplayService.submitAttempt(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Chấm điểm thành công", attempt));
    }

    @GetMapping("/attempts/history")
    @Operation(summary = "Get Attempt History", description = "View past pronunciation attempts for the current user", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<AttemptResponse>>> getAttemptHistory(Authentication authentication) {
        log.info("Getting attempt history for user: {}", authentication.getName());
        List<AttemptResponse> history = gameplayService.getAttemptHistory(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử học tập thành công", history));
    }
}
