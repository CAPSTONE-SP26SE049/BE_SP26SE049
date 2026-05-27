package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelProgressResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.service.LevelService;
import org.fsa_2026.company_fsa_captone_2026.service.QuizService;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/levels")
@RequiredArgsConstructor
@Tag(name = "Level", description = "Game Level APIs")
public class LevelController {

    private final LevelService levelService;
    private final QuizService quizService;
    private final AccountRepository accountRepository;

    /**
     * GET /api/v1/levels?dialectId= — Fix U-01/U-02 xử lý tại LevelService (404 Dialect, 400 UUID).
     */
    @GetMapping
    @Operation(summary = "Get Levels by Dialect with Progress")
    public ResponseEntity<ApiResponse<List<LevelResponse>>> getLevelsByDialect(
            Authentication authentication,
            @RequestParam("dialectId") String dialectId) {

        log.info("Get levels for dialect: {} by user: {}", dialectId,
                authentication != null ? authentication.getName() : "anonymous");

        List<LevelResponse> levels;
        if (authentication != null && authentication.isAuthenticated()) {
            Account account = accountRepository.findByEmail(authentication.getName()).orElse(null);
            if (account != null) {
                levels = levelService.getLevelsWithProgress(dialectId, account);
            } else {
                levels = levelService.getLevelsByDialect(dialectId);
            }
        } else {
            levels = levelService.getLevelsByDialect(dialectId);
        }

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cấp độ thành công", levels));
    }

    @GetMapping("/user")
    @Operation(summary = "Get User Roadmap Levels", description = "Get chapters (levels) tailored for the current user's dialect", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<LevelResponse>>> getUserLevels(
            @RequestParam(value = "dialectId", required = false) String dialectId,
            @RequestParam(value = "type", required = false) String type,
            Authentication authentication) {
        log.info("Get roadmap levels for user: {} with dialectId: {} type: {}", authentication.getName(), dialectId,
                type);
        List<LevelResponse> levels = levelService.getUserRoadmap(authentication.getName(), dialectId, type);
        return ResponseEntity
                .ok(ApiResponse.success("Lấy danh sách chương (roadmap) của người dùng thành công", levels));
    }

    /**
     * GET /api/v1/levels/{levelId}/progress — Tiến độ quiz trong một level (spec Gameplay).
     * Đã chuyển từ UserController; chỉ trả về dữ liệu của user đang đăng nhập.
     */
    @GetMapping("/{levelId}/progress")
    @Operation(summary = "Get Level Progress", description = "View quiz progress within a level: completion, scores, stars, rewards.", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<LevelProgressResponse>> getLevelProgress(
            @PathVariable("levelId") UUID levelId,
            Authentication authentication) {
        log.info("User {} getting progress for level {}", authentication.getName(), levelId);
        LevelProgressResponse response = quizService.getLevelProgress(levelId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy tiến trình level thành công", response));
    }
}
