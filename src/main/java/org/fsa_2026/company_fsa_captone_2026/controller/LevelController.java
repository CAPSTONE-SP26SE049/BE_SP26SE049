package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.service.LevelService;
import org.springframework.http.ResponseEntity;
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
@SecurityRequirement(name = "bearer-jwt")
public class LevelController {

    private final LevelService levelService;

    @GetMapping
    @Operation(summary = "Get Levels by Dialect", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<LevelResponse>>> getLevelsByDialect(
            @RequestParam String dialectId) {

        log.info("Get levels for dialect: {}", dialectId);
        List<LevelResponse> levels = levelService.getLevelsByDialect(dialectId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cấp độ thành công", levels));
    }
}
