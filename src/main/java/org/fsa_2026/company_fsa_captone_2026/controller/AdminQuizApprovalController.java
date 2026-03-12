package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ContentReviewRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizResponse;
import org.fsa_2026.company_fsa_captone_2026.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/approvals/quizzes")
@RequiredArgsConstructor
@Tag(name = "Admin Quiz Approval", description = "Admin APIs for reviewing and approving quizzes")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuizApprovalController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Get Pending Quizzes", description = "List all quizzes pending approval")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getPendingQuizzes() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài kiểm tra chờ duyệt thành công",
                adminService.getPendingQuizzes()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Quiz Details", description = "View details of a specific pending quiz")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuizById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết thành công",
                adminService.getQuizById(id)));
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "Review Quiz", description = "Approve or reject a quiz. If rejecting, provide a reason.")
    public ResponseEntity<ApiResponse<QuizResponse>> reviewQuiz(
            @PathVariable UUID id,
            @Valid @RequestBody ContentReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đánh giá bài kiểm tra thành công",
                adminService.reviewQuiz(id, request)));
    }
}
