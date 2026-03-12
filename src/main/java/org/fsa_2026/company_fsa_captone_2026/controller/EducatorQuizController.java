package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizResponse;
import org.fsa_2026.company_fsa_captone_2026.service.EducatorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/educator/quizzes")
@RequiredArgsConstructor
@Tag(name = "Educator Quizzes", description = "Educator Quiz Management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class EducatorQuizController {

    private final EducatorService educatorService;

    @GetMapping
    @Operation(summary = "Get Educator Quizzes", description = "List all quizzes managed by the educator")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getEducatorQuizzes(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài kiểm tra thành công",
                educatorService.getEducatorQuizzes(authentication.getName())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Quiz Details", description = "View quiz details including questions")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuizById(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin bài kiểm tra thành công",
                educatorService.getQuizById(authentication.getName(), id)));
    }

    @PostMapping
    @Operation(summary = "Create Quiz", description = "Create a new Input Test (Quiz) with PENDING status")
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(
            @Valid @RequestBody QuizCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo bài kiểm tra thành công",
                        educatorService.createQuiz(authentication.getName(), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Quiz", description = "Update quiz details. If approved, creates a PENDING draft.")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(
            @PathVariable UUID id,
            @Valid @RequestBody QuizCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài kiểm tra thành công",
                educatorService.updateQuiz(authentication.getName(), id, request)));
    }
}
