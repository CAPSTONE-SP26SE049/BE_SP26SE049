package org.fsa_2026.company_fsa_captone_2026.controller;

import java.util.List;
import java.util.UUID;

import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorDashboardSummaryResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ErrorTagResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.FeedbackCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.PlacementRuleRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.PlacementRuleResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.StudentAnalyticsResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorActivityResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ContentApprovalHistoryResponse;
import org.fsa_2026.company_fsa_captone_2026.service.EducatorService;
import org.fsa_2026.company_fsa_captone_2026.service.ErrorTagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/educator")
@RequiredArgsConstructor
@Tag(name = "Educator", description = "Educator Portal APIs")
@SecurityRequirement(name = "bearer-jwt")
public class EducatorController {

        private static final String MSG_SUCCESS = "Thành công";

        private final EducatorService educatorService;
        private final ErrorTagService errorTagService;

        @GetMapping("/dashboard/summary")
        @Operation(summary = "Get Dashboard Summary", description = "Overview stats for educator")
        public ResponseEntity<ApiResponse<EducatorDashboardSummaryResponse>> getDashboardSummary(
                        Authentication authentication) {
                return ResponseEntity
                                .ok(ApiResponse.success(MSG_SUCCESS,
                                                educatorService.getDashboardSummary(authentication.getName())));
        }

        @GetMapping("/dashboard/activities")
        @Operation(summary = "Get Recent Activities", description = "Monitor latest student exercise attempts")
        public ResponseEntity<ApiResponse<List<EducatorActivityResponse>>> getRecentActivities(
                        Authentication authentication) {
                return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS,
                                educatorService.getRecentActivities(authentication.getName())));
        }



        @GetMapping("/curriculum/{region}")
        @Operation(summary = "Get Curriculum by Region", description = "List levels filtered by region")
        public ResponseEntity<ApiResponse<List<LevelResponse>>> getCurriculumByRegion(
                        @PathVariable String region,
                        Authentication authentication) {
                return ResponseEntity.ok(
                                ApiResponse.success(MSG_SUCCESS, educatorService.getCurriculumByRegion(region)));
        }

        @PostMapping("/curriculum/levels")
        @Operation(summary = "Create Level", description = "Create a new learning level")
        public ResponseEntity<ApiResponse<LevelResponse>> createLevel(
                        @Valid @RequestBody LevelCreateRequest request,
                        Authentication authentication) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Tạo cấp độ thành công",
                                                educatorService.createLevel(authentication.getName(), request)));
        }

        @GetMapping("/content/{id}/history")
        @Operation(summary = "Get Content Approval History", description = "View the audit log for a specific level or challenge")
        public ResponseEntity<ApiResponse<List<ContentApprovalHistoryResponse>>> getContentApprovalHistory(
                        @PathVariable UUID id) {
                return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử duyệt thành công",
                                educatorService.getContentApprovalHistory(id)));
        }

        @PatchMapping("/curriculum/levels/{levelId}")
        @Operation(summary = "Update Level", description = "Update level details like title and threshold")
        public ResponseEntity<ApiResponse<LevelResponse>> updateLevel(
                        @PathVariable UUID levelId,
                        @Valid @RequestBody LevelCreateRequest request,
                        Authentication authentication) {
                return ResponseEntity.ok(
                                ApiResponse.success("Cập nhật cấp độ thành công",
                                                educatorService.updateLevel(authentication.getName(), levelId,
                                                                request)));
        }

        @DeleteMapping("/curriculum/levels/{levelId}")
        @Operation(summary = "Delete Level", description = "Delete a level and cascade delete its challenges")
        public ResponseEntity<ApiResponse<Void>> deleteLevel(
                        @PathVariable UUID levelId,
                        Authentication authentication) {
                educatorService.deleteLevel(authentication.getName(), levelId);
                return ResponseEntity.ok(ApiResponse.success("Level deleted successfully", null));
        }

        @PostMapping("/curriculum/challenges")
        @Operation(summary = "Create Challenge", description = "Create a new pronunciation challenge")
        public ResponseEntity<ApiResponse<ChallengeResponse>> createChallenge(
                        @Valid @RequestBody ChallengeCreateRequest request,
                        Authentication authentication) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Đã thêm thử thách mới thành công",
                                                educatorService.createChallenge(authentication.getName(), request)));
        }

        @PutMapping("/curriculum/challenges/{challengeId}")
        @Operation(summary = "Update Challenge", description = "Update an existing challenge by ID")
        public ResponseEntity<ApiResponse<ChallengeResponse>> updateChallenge(
                        @PathVariable UUID challengeId,
                        @Valid @RequestBody ChallengeCreateRequest request,
                        Authentication authentication) {
                return ResponseEntity.ok(ApiResponse.success("Cập nhật thử thách thành công",
                                educatorService.updateChallenge(authentication.getName(), challengeId, request)));
        }

        @DeleteMapping("/curriculum/challenges/{id}")
        @Operation(summary = "Delete Challenge", description = "Delete a challenge by ID")
        public ResponseEntity<ApiResponse<Void>> deleteChallenge(
                        @PathVariable UUID id,
                        Authentication authentication) {
                educatorService.deleteChallenge(authentication.getName(), id);
                return ResponseEntity.ok(ApiResponse.success("Xóa thử thách thành công", null));
        }

        @PostMapping("/curriculum/levels/{levelId}/audio")
        @Operation(summary = "Upload Level Audio", description = "Upload reference audio for a level")
        public ResponseEntity<ApiResponse<Void>> uploadLevelAudio(
                        @PathVariable UUID levelId,
                        @RequestParam String audioUrl) {
                educatorService.uploadLevelAudio(levelId, audioUrl);
                return ResponseEntity.ok(ApiResponse.success("Tải âm thanh mẫu thành công", null));
        }

        @GetMapping("/curriculum/error-tags")
        @Operation(summary = "Get All Error Tags", description = "Retrieve list of all error tags (optional: filter by dialect)")
        public ResponseEntity<ApiResponse<List<ErrorTagResponse>>> getErrorTags(
                        @RequestParam(required = false) UUID dialectId) {
                return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mã lỗi thành công",
                                errorTagService.getErrorTagsByDialect(dialectId)));
        }


        @GetMapping("/placement/rules")
        @Operation(summary = "Get Placement Rules", description = "List all student routing rules")
        public ResponseEntity<ApiResponse<List<PlacementRuleResponse>>> getPlacementRules(
                        Authentication authentication) {
                return ResponseEntity.ok(
                                ApiResponse.success(MSG_SUCCESS, educatorService.getPlacementRules()));
        }

        @PostMapping("/placement/rules")
        @Operation(summary = "Update/Create Placement Rule", description = "Configure evaluation thresholds")
        public ResponseEntity<ApiResponse<PlacementRuleResponse>> updatePlacementRule(
                        @Valid @RequestBody PlacementRuleRequest request,
                        Authentication authentication) {
                return ResponseEntity.ok(
                                ApiResponse.success("Cấu hình quy tắc thành công",
                                                educatorService.updateOrCreatePlacementRule(request)));
        }

        @GetMapping("/levels")
        @Operation(summary = "Get Levels", description = "List all approved levels")
        public ResponseEntity<ApiResponse<List<LevelResponse>>> getLevelsForSelection() {
                return ResponseEntity.ok(
                                ApiResponse.success(MSG_SUCCESS, educatorService.getAllLevelsForSelection()));
        }
}
