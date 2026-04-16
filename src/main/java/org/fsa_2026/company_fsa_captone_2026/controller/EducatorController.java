package org.fsa_2026.company_fsa_captone_2026.controller;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.service.EducatorService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/educator")
@RequiredArgsConstructor
public class EducatorController {

    private static final String MSG_SUCCESS = "Thành công";

    private final EducatorService educatorService;

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardSummary(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS, educatorService.getDashboardSummary(authentication.getName())));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStudents(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS, educatorService.getStudentAccounts(authentication.getName())));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStudentById(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS, educatorService.getStudentAccountById(authentication.getName(), id)));
    }

    @PostMapping("/students/learning-paths")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createLearningPath(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        UUID studentId = UUID.fromString(String.valueOf(request.get("studentId")));
        String title = String.valueOf(request.get("title"));
        String focusArea = String.valueOf(request.get("focusArea"));
        @SuppressWarnings("unchecked")
        List<String> milestones = (List<String>) request.getOrDefault("milestones", List.of());
        String description = request.get("description") != null ? String.valueOf(request.get("description")) : null;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MSG_SUCCESS,
                        educatorService.createCustomLearningPath(authentication.getName(), studentId, title, focusArea, milestones, description)));
    }

    @GetMapping("/progress/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgressOverview(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS, educatorService.getProgressOverview(authentication.getName())));
    }

    @GetMapping("/progress/pronunciation-analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPronunciationAnalytics(
            @RequestParam(required = false) UUID studentId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS, educatorService.getPronunciationAnalytics(authentication.getName(), studentId)));
    }

    @GetMapping("/lessons")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLessonPlans(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS, educatorService.getLessonPlans(authentication.getName())));
    }

    @PostMapping("/lessons")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createLessonPlan(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        String title = String.valueOf(request.get("title"));
        String objective = String.valueOf(request.get("objective"));
        @SuppressWarnings("unchecked")
        List<String> targetStudents = (List<String>) request.getOrDefault("targetStudents", List.of());
        @SuppressWarnings("unchecked")
        List<String> achievementGoals = (List<String>) request.getOrDefault("achievementGoals", List.of());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MSG_SUCCESS,
                        educatorService.createLessonPlan(authentication.getName(), title, objective, targetStudents, achievementGoals)));
    }

    @PatchMapping("/lessons/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateLessonPlan(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        String title = String.valueOf(request.getOrDefault("title", ""));
        String objective = String.valueOf(request.getOrDefault("objective", ""));
        @SuppressWarnings("unchecked")
        List<String> targetStudents = (List<String>) request.getOrDefault("targetStudents", List.of());
        @SuppressWarnings("unchecked")
        List<String> achievementGoals = (List<String>) request.getOrDefault("achievementGoals", List.of());
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS,
                educatorService.updateLessonPlan(authentication.getName(), id, title, objective, targetStudents, achievementGoals)));
    }

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMessages(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS, educatorService.getMessages(authentication.getName())));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        UUID studentId = UUID.fromString(String.valueOf(request.get("studentId")));
        String content = String.valueOf(request.get("content"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MSG_SUCCESS, educatorService.sendMessage(authentication.getName(), studentId, content)));
    }

    @GetMapping("/feedback")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFeedback(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS, educatorService.getFeedbackItems(authentication.getName())));
    }

    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendFeedback(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        UUID studentId = UUID.fromString(String.valueOf(request.get("studentId")));
        String content = String.valueOf(request.get("content"));
        String priority = String.valueOf(request.getOrDefault("priority", "MEDIUM"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MSG_SUCCESS, educatorService.sendFeedback(authentication.getName(), studentId, content, priority)));
    }

    @GetMapping("/analytics/reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAnalyticsReports(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS, educatorService.getAnalyticsReports(authentication.getName())));
    }

    @GetMapping("/analytics/reports/{studentId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsReportByStudent(@PathVariable UUID studentId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS, educatorService.getAnalyticsReportByStudent(authentication.getName(), studentId)));
    }
}
