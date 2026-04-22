package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.CustomPathDtos.*;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.service.CustomLearningPathService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX)
@RequiredArgsConstructor
@Tag(name = "Custom Learning Path", description = "Endpoints for managing personalized learning paths")
public class CustomPathController {

    private final CustomLearningPathService pathService;
    private final LearningUnitRepository learningUnitRepository;
    private final AccountRepository accountRepository;

    // ─── EDUCATOR ENDPOINTS ──────────────────

    @GetMapping("/educator/all-levels")
    @PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")
    @Operation(summary = "Get All Levels", description = "Educator gets all available chapters for custom path selection")
    public ResponseEntity<ApiResponse<List<PathLevelResponse>>> getAllLevels() {
        List<LearningUnit> levels = learningUnitRepository.findByType("LEVEL");
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        List<PathLevelResponse> responses = levels.stream()
                .sorted(Comparator.comparingInt(l -> {
                    try {
                        if (l.getMetadataJson() != null) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> meta = om.readValue(l.getMetadataJson(), java.util.Map.class);
                            Object order = meta.get("orderIndex");
                            if (order == null) order = meta.get("level_order");
                            
                            if (order instanceof Number) return ((Number) order).intValue();
                            if (order instanceof String) return Integer.parseInt((String) order);
                        }
                    } catch (Exception ignored) {}
                    return 0;
                }))
                .map(l -> PathLevelResponse.builder()
                        .levelId(l.getId())
                        .levelName(l.getName())
                        .region(l.getParent() != null ? l.getParent().getName() : "Unknown")
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chương thành công", responses));
    }


    @PostMapping("/educator/students/{studentId}/custom-path")
    @PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")
    @Operation(summary = "Create Custom Path", description = "Educator creates or updates a custom path for a student")
    public ResponseEntity<ApiResponse<CustomPathResponse>> createPath(
            @PathVariable UUID studentId,
            @RequestBody CreateCustomPathRequest request,
            Authentication authentication) {
        log.info("Educator {} creating custom path for student {}", authentication.getName(), studentId);
        CustomPathResponse response = pathService.createCustomPath(authentication.getName(), studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Tạo lộ trình thành công", response));
    }

    @GetMapping("/educator/students/{studentId}/custom-path")
    @PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomPathResponse>> getPathForEducator(@PathVariable UUID studentId) {
        return ResponseEntity
                .ok(ApiResponse.success("Lấy lộ trình thành công", pathService.getActivePathForStudent(studentId)));
    }

    // ─── LEARNER ENDPOINTS ───────────────────

    @GetMapping("/learner/custom-path")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get My Custom Path", description = "Student retrieves their current personalized learning path", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<CustomPathResponse>> getMyPath(Authentication authentication) {
        Account user = accountRepository.findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity
                .ok(ApiResponse.success("Lấy lộ trình thành công", pathService.getActivePathForStudent(user.getId())));
    }

    @PostMapping("/learner/custom-path/quizzes/{quizId}/complete")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Submit Custom Path Progress", description = "Submit results for a quiz played within the custom path")
    public ResponseEntity<ApiResponse<String>> submitProgress(
            @PathVariable UUID quizId,
            @RequestBody SubmitCustomProgressRequest request,
            Authentication authentication) {
        Account user = accountRepository.findByEmail(authentication.getName()).orElseThrow();
        pathService.submitProgress(user.getId(), quizId, request.getScore());
        return ResponseEntity.ok(ApiResponse.success("Lưu tiến độ thành công", "Progress saved"));
    }
}
