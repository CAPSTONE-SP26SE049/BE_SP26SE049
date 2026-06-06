package org.fsa_2026.company_fsa_captone_2026.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserFeedbackDtos.*;
import org.fsa_2026.company_fsa_captone_2026.service.UserFeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserFeedbackController {

    private static final String MSG_SUCCESS = "Thành công";

    private final UserFeedbackService userFeedbackService;

    // Learner: gửi feedback
    @PostMapping("/api/v1/user-feedback")
    public ResponseEntity<ApiResponse<UserFeedbackResponse>> createFeedback(
            @RequestBody CreateUserFeedbackRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi phản hồi thành công",
                        userFeedbackService.createFeedback(request, authentication.getName())));
    }

    // Learner: xem feedback của mình
    @GetMapping("/api/v1/user-feedback/my")
    public ResponseEntity<ApiResponse<List<UserFeedbackResponse>>> getMyFeedbacks(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS,
                userFeedbackService.getMyFeedbacks(authentication.getName())));
    }

    // Admin: xem tất cả feedback
    @GetMapping("/api/v1/admin/user-feedbacks")
    public ResponseEntity<ApiResponse<PagedFeedbackResponse>> getAllFeedbacks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(MSG_SUCCESS,
                userFeedbackService.getAllFeedbacks(status, category, page, size)));
    }

    // Admin: cập nhật trạng thái + ghi chú
    @PatchMapping("/api/v1/admin/user-feedbacks/{id}/status")
    public ResponseEntity<ApiResponse<UserFeedbackResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateFeedbackStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công",
                userFeedbackService.updateStatus(id, request)));
    }
}
