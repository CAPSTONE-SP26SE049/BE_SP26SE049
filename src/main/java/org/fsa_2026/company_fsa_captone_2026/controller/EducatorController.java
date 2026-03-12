package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.*;
import org.fsa_2026.company_fsa_captone_2026.service.EducatorService;
import org.fsa_2026.company_fsa_captone_2026.service.ErrorTagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/educator")
@RequiredArgsConstructor
@Tag(name = "Educator", description = "Educator Portal APIs")
@SecurityRequirement(name = "bearer-jwt")
public class EducatorController {

        private final EducatorService educatorService;
        private final ErrorTagService errorTagService;

        @GetMapping("/dashboard/summary")
        @Operation(summary = "Get Dashboard Summary", description = "Overview stats for educator")
        public ResponseEntity<ApiResponse<EducatorDashboardSummaryResponse>> getDashboardSummary(
                        Authentication authentication) {
                return ResponseEntity
                                .ok(ApiResponse.success("Thành công",
                                                educatorService.getDashboardSummary(authentication.getName())));
        }

        @GetMapping("/classrooms")
        @Operation(summary = "Get Classrooms", description = "List all classrooms managed by the educator")
        public ResponseEntity<ApiResponse<List<ClassroomResponse>>> getClassrooms(Authentication authentication) {
                return ResponseEntity
                                .ok(ApiResponse.success("Thành công",
                                                educatorService.getClassrooms(authentication.getName())));
        }

        @PostMapping("/classrooms")
        @Operation(summary = "Create Classroom", description = "Create a new classroom")
        public ResponseEntity<ApiResponse<ClassroomResponse>> createClassroom(
                        Authentication authentication,
                        @Valid @RequestBody ClassroomCreateRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Tạo lớp học thành công",
                                                educatorService.createClassroom(authentication.getName(), request)));
        }

        @PatchMapping("/classrooms/{id}")
        @Operation(summary = "Update Classroom", description = "Update classroom name")
        public ResponseEntity<ApiResponse<ClassroomResponse>> updateClassroom(
                        @PathVariable UUID id,
                        @Valid @RequestBody ClassroomCreateRequest request,
                        Authentication authentication) {
                return ResponseEntity.ok(ApiResponse.success("Cập nhật lớp học thành công",
                                educatorService.updateClassroom(authentication.getName(), id, request)));
        }

        @DeleteMapping("/classrooms/{id}")
        @Operation(summary = "Delete Classroom", description = "Remove a classroom")
        public ResponseEntity<ApiResponse<Void>> deleteClassroom(
                        @PathVariable UUID id,
                        Authentication authentication) {
                educatorService.deleteClassroom(authentication.getName(), id);
                return ResponseEntity.ok(ApiResponse.success("Xóa lớp học thành công", null));
        }

        @GetMapping("/classrooms/{id}/students")
        @Operation(summary = "Get Classroom Students", description = "List all students in a classroom")
        public ResponseEntity<ApiResponse<List<UserManagementResponse>>> getClassroomStudents(
                        @PathVariable UUID id,
                        Authentication authentication) {
                return ResponseEntity.ok(
                                ApiResponse.success("Thành công",
                                                educatorService.getClassroomStudents(authentication.getName(), id)));
        }

        @PostMapping("/classrooms/{id}/students")
        @Operation(summary = "Add Student to Classroom", description = "Enroll a student using email")
        public ResponseEntity<ApiResponse<Void>> addStudentToClassroom(
                        @PathVariable UUID id,
                        @Valid @RequestBody AddStudentRequest request,
                        Authentication authentication) {
                educatorService.addStudentToClassroom(authentication.getName(), id, request);
                return ResponseEntity.ok(ApiResponse.success("Thêm học viên vào lớp thành công", null));
        }

        @DeleteMapping("/classrooms/{id}/students/{studentId}")
        @Operation(summary = "Remove Student from Classroom", description = "Unenroll a student")
        public ResponseEntity<ApiResponse<Void>> removeStudentFromClassroom(
                        @PathVariable UUID id,
                        @PathVariable UUID studentId,
                        Authentication authentication) {
                educatorService.removeStudentFromClassroom(authentication.getName(), id, studentId);
                return ResponseEntity.ok(ApiResponse.success("Xóa học viên khỏi lớp thành công", null));
        }

        @GetMapping("/students/{id}/analytics")
        @Operation(summary = "Get Student Analytics", description = "Detailed pronunciation report for a student")
        public ResponseEntity<ApiResponse<StudentAnalyticsResponse>> getStudentAnalytics(
                        @PathVariable UUID id,
                        Authentication authentication) {
                return ResponseEntity.ok(
                                ApiResponse.success("Thành công",
                                                educatorService.getStudentAnalytics(authentication.getName(), id)));
        }

        @GetMapping("/classrooms/{id}/performance")
        @Operation(summary = "Get Classroom Performance", description = "Aggregated performance stats for a classroom")
        public ResponseEntity<ApiResponse<ClassroomPerformanceResponse>> getClassroomPerformance(
                        @PathVariable UUID id,
                        Authentication authentication) {
                return ResponseEntity.ok(
                                ApiResponse.success("Thành công",
                                                educatorService.getClassroomPerformance(authentication.getName(), id)));
        }

        @GetMapping("/curriculum/{region}")
        @Operation(summary = "Get Curriculum by Region", description = "List levels filtered by region")
        public ResponseEntity<ApiResponse<List<LevelResponse>>> getCurriculumByRegion(
                        @PathVariable String region,
                        Authentication authentication) {
                return ResponseEntity.ok(
                                ApiResponse.success("Thành công", educatorService.getCurriculumByRegion(region)));
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
        public ResponseEntity<ApiResponse<List<org.fsa_2026.company_fsa_captone_2026.dto.ContentApprovalHistoryResponse>>> getContentApprovalHistory(
                        @PathVariable UUID id) {
                return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử duyệt thành công",
                                educatorService.getContentApprovalHistory(id)));
        }

        @PatchMapping("/curriculum/levels/{levelId}")
        @Operation(summary = "Update Level", description = "Update level details like title and threshold")
        public ResponseEntity<ApiResponse<LevelResponse>> updateLevel(
                        @PathVariable UUID levelId,
                        @Valid @RequestBody LevelUpdateRequest request,
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

        @PostMapping("/students/{id}/feedback")
        @Operation(summary = "Submit Student Feedback", description = "Teacher provides manual feedback on an attempt")
        public ResponseEntity<ApiResponse<Void>> submitFeedback(
                        @PathVariable UUID id,
                        @Valid @RequestBody FeedbackCreateRequest request,
                        Authentication authentication) {
                educatorService.submitFeedback(authentication.getName(), id, request);
                return ResponseEntity.ok(ApiResponse.success("Gửi phản hồi thành công", null));
        }

        @GetMapping("/placement/rules")
        @Operation(summary = "Get Placement Rules", description = "List all student routing rules")
        public ResponseEntity<ApiResponse<List<PlacementRuleResponse>>> getPlacementRules(
                        Authentication authentication) {
                return ResponseEntity.ok(
                                ApiResponse.success("Thành công", educatorService.getPlacementRules()));
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
        @Operation(summary = "Get Levels for Selection", description = "List all approved levels for quiz assignment")
        public ResponseEntity<ApiResponse<List<LevelSelectionResponse>>> getLevelsForSelection() {
                return ResponseEntity.ok(
                                ApiResponse.success("Thành công", educatorService.getAllLevelsForSelection()));
        }
}
