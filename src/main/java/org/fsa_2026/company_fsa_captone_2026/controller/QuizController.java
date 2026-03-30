package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelProgressResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCompleteRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCompleteResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Quizzes", description = "Quiz APIs")
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/quizzes")
    @Operation(summary = "Create Quiz", description = "Create a quiz stored as LearningUnit metadata")
    public ResponseEntity<ApiResponse<LearningUnit>> createQuiz(@Valid @RequestBody QuizCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo quiz thành công", quizService.createQuiz(request)));
    }

    @GetMapping("/quizzes")
    @Operation(summary = "Get All Quizzes", description = "Fetch all quizzes with metadata parsed")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllQuizzes() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quiz thành công", quizService.getAllQuizzes()));
    }

    @GetMapping("/levels/{levelId}/quizzes")
    @Operation(summary = "Get Quizzes by Level", description = "Fetch quizzes inside a specific level for user selection", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuizzesByLevel(@PathVariable UUID levelId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quiz theo level thành công", quizService.getQuizzesByLevel(levelId)));
    }

}

