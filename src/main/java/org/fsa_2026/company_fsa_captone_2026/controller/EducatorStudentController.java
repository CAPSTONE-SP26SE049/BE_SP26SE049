package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorQuestRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.StudentAnalyticsResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.StudentProfileResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LearnerDashboardResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserManagementResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Quest;
import org.fsa_2026.company_fsa_captone_2026.entity.StudentAssignment;
import org.fsa_2026.company_fsa_captone_2026.dto.FeedbackCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.EducatorFeedback;
import org.fsa_2026.company_fsa_captone_2026.service.EducatorService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/educator/students")
@RequiredArgsConstructor
@Tag(name = "Educator Students", description = "Student management for Educators")
@PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")
public class EducatorStudentController {

    private final EducatorService educatorService;

    @GetMapping
    @Operation(summary = "Get Assigned Students", description = "List all students assigned to or manageable by this educator")
    public ResponseEntity<ApiResponse<List<UserManagementResponse>>> getAssignedStudents(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Fetched assigned students", educatorService.getAssignedStudents(auth.getName())));
    }

    @GetMapping("/{studentId}")
    @Operation(summary = "Get Student Profile", description = "Fetch comprehensive student profile with aggregate stats and active assignments")
    public ResponseEntity<ApiResponse<StudentProfileResponse>> getStudentProfile(Authentication auth, @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success("Fetched student profile", educatorService.getStudentProfile(auth.getName(), studentId)));
    }

    @GetMapping("/{studentId}/analytics")
    @Operation(summary = "Get Student Analytics", description = "Fetch detailed pronunciation, progress analytics, and score trends for a student")
    public ResponseEntity<ApiResponse<StudentAnalyticsResponse>> getStudentAnalytics(Authentication auth, @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success("Fetched student analytics", educatorService.getStudentAnalytics(auth.getName(), studentId)));
    }

    @GetMapping("/{studentId}/feedback")
    @Operation(summary = "Get Student Feedback History", description = "Fetch all previous feedback given to this student")
    public ResponseEntity<ApiResponse<List<EducatorFeedback>>> getFeedbackHistory(Authentication auth, @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success("Fetched feedback history", educatorService.getStudentFeedbackHistory(auth.getName(), studentId)));
    }

    @PatchMapping("/{studentId}/status")
    @Operation(summary = "Update Student Status", description = "Enable or disable a student account")
    public ResponseEntity<ApiResponse<Void>> updateStatus(Authentication auth, @PathVariable UUID studentId, @RequestParam boolean isActive) {
        educatorService.updateStudentStatus(auth.getName(), studentId, isActive);
        return ResponseEntity.ok(ApiResponse.success("Student status updated", null));
    }

    @PostMapping("/{studentId}/feedback")
    @Operation(summary = "Submit Student Feedback", description = "Teacher provides manual feedback on an attempt")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            Authentication auth,
            @PathVariable UUID studentId,
            @Valid @RequestBody FeedbackCreateRequest request) {
        educatorService.submitFeedback(auth.getName(), studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Gửi phản hồi thành công", null));
    }

    @PostMapping("/{studentId}/assignments")
    @Operation(summary = "Assign Lesson to Student", description = "Create a custom learning path entry by assigning a learning unit")
    public ResponseEntity<ApiResponse<StudentAssignment>> assignLesson(
            Authentication auth,
            @PathVariable UUID studentId,
            @RequestParam UUID unitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline) {
        return ResponseEntity.ok(ApiResponse.success("Lesson assigned to student", educatorService.assignLessonToStudent(auth.getName(), studentId, unitId, deadline)));
    }

    @GetMapping("/{studentId}/quests")
    @Operation(summary = "Get Student Quest Progress", description = "Monitor student progress on global and personalized achievement goals")
    public ResponseEntity<ApiResponse<List<LearnerDashboardResponse.DailyQuest>>> getStudentQuests(Authentication auth, @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success("Fetched student quest progress", educatorService.getStudentQuests(auth.getName(), studentId)));
    }

    @PostMapping("/{studentId}/quests")
    @Operation(summary = "Assign Quest to Student", description = "Assign a personalized achievement goal to a specific student")
    public ResponseEntity<ApiResponse<Quest>> assignQuest(
            Authentication auth,
            @PathVariable UUID studentId,
            @RequestBody EducatorQuestRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Quest assigned to student", educatorService.assignQuestToStudent(auth.getName(), studentId, request)));
    }
}
