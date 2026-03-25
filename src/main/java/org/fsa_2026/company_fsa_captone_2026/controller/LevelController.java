package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.service.LevelService;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
<<<<<<< HEAD
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
=======
>>>>>>> 123456b4ec8b41d914d253da4e8e138ffab643d7
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/levels")
@RequiredArgsConstructor
@Tag(name = "Level", description = "Game Level APIs")
public class LevelController {

    private final LevelService levelService;
    private final AccountRepository accountRepository;

    @GetMapping
    @Operation(summary = "Get Levels by Dialect with Progress")
    public ResponseEntity<ApiResponse<List<LevelResponse>>> getLevelsByDialect(
            Authentication authentication,
            @RequestParam String dialectId) {

        log.info("Get levels for dialect: {} by user: {}", dialectId, authentication != null ? authentication.getName() : "anonymous");
        
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
            @RequestParam(required = false) String dialectId,
            Authentication authentication) {
        log.info("Get roadmap levels for user: {} with dialectId: {}", authentication.getName(), dialectId);
        List<LevelResponse> levels = levelService.getUserRoadmap(authentication.getName(), dialectId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chương (roadmap) của người dùng thành công", levels));
    }
}
