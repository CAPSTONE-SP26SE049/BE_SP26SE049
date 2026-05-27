package org.fsa_2026.company_fsa_captone_2026.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserManagementResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.FeedbackDtos.*;
import org.fsa_2026.company_fsa_captone_2026.service.EducatorService;
import org.fsa_2026.company_fsa_captone_2026.service.FeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/educator")
@RequiredArgsConstructor
@Slf4j
public class EducatorController {

        private static final String MSG_SUCCESS = "Thành công";

        private final EducatorService educatorService;
        private final FeedbackService feedbackService;

        @GetMapping("/dashboard/summary")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary(Authentication authentication) {
                return ResponseEntity
                                .ok(ApiResponse.success(MSG_SUCCESS,
                                                educatorService.getDashboardSummary(authentication.getName())));
        }

        @GetMapping("/students")
        public ResponseEntity<ApiResponse<List<UserManagementResponse>>> getStudents(Authentication authentication) {
                return ResponseEntity
                                .ok(ApiResponse.success(MSG_SUCCESS,
                                                educatorService.getStudentAccounts(authentication.getName())));
        }

        @GetMapping("/students/{id}")
        public ResponseEntity<ApiResponse<UserManagementResponse>> getStudentById(@PathVariable("id") UUID id,
                        Authentication authentication) {
                return ResponseEntity.ok(
                                ApiResponse.success(MSG_SUCCESS,
                                                educatorService.getStudentAccountById(authentication.getName(), id)));
        }

        @PostMapping("/students/learning-paths")
        public ResponseEntity<ApiResponse<Map<String, Object>>> createLearningPath(
                        @Valid @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.EducatorJourneyDtos.CustomLearningPathRequest request,
                        Authentication authentication) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(MSG_SUCCESS,
                                                educatorService.createCustomLearningPath(authentication.getName(),
                                                                request.getStudentId(), request.getTitle(), request.getFocusArea(),
                                                                request.getMilestones(), request.getDescription())));
        }

        @GetMapping("/progress/overview")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getProgressOverview(Authentication authentication) {
                return ResponseEntity
                                .ok(ApiResponse.success(MSG_SUCCESS,
                                                educatorService.getProgressOverview(authentication.getName())));
        }

        @GetMapping("/progress/pronunciation-analytics")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getPronunciationAnalytics(
                        @RequestParam(value = "studentId", required = false) UUID studentId,
                        Authentication authentication) {
                return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS,
                                educatorService.getPronunciationAnalytics(authentication.getName(), studentId)));
        }

        @GetMapping("/lessons")
        public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLessonPlans(Authentication authentication) {
                return ResponseEntity
                                .ok(ApiResponse.success(MSG_SUCCESS,
                                                educatorService.getLessonPlans(authentication.getName())));
        }

        @PostMapping("/lessons")
        public ResponseEntity<ApiResponse<Map<String, Object>>> createLessonPlan(
                        @Valid @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.EducatorJourneyDtos.LessonPlanRequest request,
                        Authentication authentication) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(MSG_SUCCESS,
                                                educatorService.createLessonPlan(authentication.getName(), request.getTitle(),
                                                                request.getObjective(), request.getTargetStudents(),
                                                                request.getAchievementGoals())));
        }

        @PatchMapping("/lessons/{id}")
        public ResponseEntity<ApiResponse<Map<String, Object>>> updateLessonPlan(
                        @PathVariable("id") UUID id,
                        @RequestBody Map<String, Object> request,
                        Authentication authentication) {
                String title = request.get("title") != null ? String.valueOf(request.get("title")) : null;
                String objective = request.get("objective") != null ? String.valueOf(request.get("objective")) : null;
                @SuppressWarnings("unchecked")
                List<String> targetStudents = request.get("targetStudents") != null ? (List<String>) request.get("targetStudents") : null;
                @SuppressWarnings("unchecked")
                List<String> achievementGoals = request.get("achievementGoals") != null ? (List<String>) request.get("achievementGoals") : null;
                return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS,
                                educatorService.updateLessonPlan(authentication.getName(), id, title, objective,
                                                targetStudents,
                                                achievementGoals)));
        }

        @GetMapping("/messages/{studentId}")
        public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMessages(
                        @PathVariable("studentId") String studentId,
                        Authentication authentication) {
                try {
                        String cleanId = studentId.trim();
                        UUID sId = UUID.fromString(cleanId);
                        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS,
                                        educatorService.getConversationMessages(authentication.getName(), sId)));
                } catch (IllegalArgumentException e) {
                        log.error("Invalid student UUID format: '{}'", studentId);
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error("ID học viên không hợp lệ: " + studentId
                                                        + ". Chi tiết: " + e.getMessage()));
                } catch (Exception e) {
                        log.error("Error fetching messages for student {}: ", studentId, e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ApiResponse.error("Lỗi hệ thống khi tải tin nhắn: " + e.getMessage()));
                }
        }

        @PostMapping("/messages")
        public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
                        @RequestBody Map<String, Object> request,
                        Authentication authentication) {
                UUID studentId = UUID.fromString(String.valueOf(request.get("studentId")));
                String content = String.valueOf(request.get("content"));
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(MSG_SUCCESS,
                                                educatorService.sendMessage(authentication.getName(), studentId,
                                                                content)));
        }

        @GetMapping("/analytics/reports")
        public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAnalyticsReports(
                        Authentication authentication) {
                return ResponseEntity
                                .ok(ApiResponse.success(MSG_SUCCESS,
                                                educatorService.getAnalyticsReports(authentication.getName())));
        }

        @GetMapping("/analytics/reports/{studentId}")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsReportByStudent(
                        @PathVariable("studentId") UUID studentId,
                        Authentication authentication) {
                return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS,
                                educatorService.getAnalyticsReportByStudent(authentication.getName(), studentId)));
        }

        @GetMapping("/feedback")
        public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFeedbackItems(Authentication authentication) {
                return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS,
                                educatorService.getFeedbackItems(authentication.getName())));
        }

        // Feedback & Interaction Endpoints
        @PostMapping("/feedback")
        public ResponseEntity<ApiResponse<FeedbackResponse>> sendFeedback(
                        @Valid @RequestBody CreateFeedbackRequest request,
                        Authentication authentication) {
                return ResponseEntity.ok(ApiResponse.success("Gửi phản hồi thành công",
                                feedbackService.sendFeedback(request, authentication.getName())));
        }

        @GetMapping("/interactions/attempts/{studentId}")
        @PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")
        public ResponseEntity<ApiResponse<List<SpeakingAttemptResponse>>> getStudentSpeakingAttempts(
                        @PathVariable("studentId") UUID studentId,
                        Authentication authentication) {
                return ResponseEntity.ok(ApiResponse.success(feedbackService.getRecentSpeakingAttempts(studentId)));
        }

        @GetMapping("/feedback/student/{studentId}")
        @PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")
        public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getStudentFeedbacks(
                        @PathVariable("studentId") UUID studentId) {
                return ResponseEntity.ok(ApiResponse.success(feedbackService.getStudentFeedbacks(studentId)));
        }

        @PostMapping("/messages/read/{studentId}")
        public ResponseEntity<ApiResponse<Void>> markAsRead(
                        @PathVariable("studentId") UUID studentId,
                        Authentication authentication) {
                educatorService.markAsRead(authentication.getName(), studentId);
                return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc", null));
        }
}
