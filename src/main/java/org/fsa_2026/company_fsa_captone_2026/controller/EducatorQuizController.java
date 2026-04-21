package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/educator/quizzes")
@RequiredArgsConstructor
@Tag(name = "Educator Quizzes", description = "Educator Quiz Management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class EducatorQuizController {

    private final QuizService quizService;

    @GetMapping
    @Operation(summary = "Get quizzes by level", description = "Fetch quizzes stored in LearningUnit by levelId")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuizzesByLevel(
            @RequestParam("levelId") UUID levelId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quiz thành công", quizService.getQuizzesByLevel(levelId)));
    }

    @PostMapping
    @Operation(summary = "Create Quiz", description = "Create a new Input Test (Quiz) stored in LearningUnit")
    public ResponseEntity<ApiResponse<LearningUnit>> createQuiz(
            @Valid @RequestBody QuizCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo quiz thành công", quizService.createQuiz(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Quiz", description = "Update an existing Quiz stored in LearningUnit")
    public ResponseEntity<ApiResponse<LearningUnit>> updateQuiz(
            @PathVariable("id") UUID id,
            @Valid @RequestBody QuizCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quiz thành công", quizService.updateQuiz(id, request)));
    }
}
