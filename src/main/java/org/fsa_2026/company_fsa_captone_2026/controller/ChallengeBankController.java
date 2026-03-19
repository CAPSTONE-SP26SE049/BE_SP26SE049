package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeAssignRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeBankRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem;
import org.fsa_2026.company_fsa_captone_2026.service.ChallengeBankService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/educator")
@RequiredArgsConstructor
@Tag(name = "Educator Challenge Bank", description = "Quản lý kho câu hỏi cho educator")
@SecurityRequirement(name = "bearer-jwt")
public class ChallengeBankController {

    private final ChallengeBankService challengeBankService;

    @PostMapping("/challenge-bank")
    @Operation(summary = "Tạo câu hỏi mới trong kho")
    public ResponseEntity<ApiResponse<ChallengeBank>> createChallenge(
            @Valid @RequestBody ChallengeBankRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo câu hỏi thành công", challengeBankService.createChallenge(request)));
    }

    @GetMapping("/challenge-bank")
    @Operation(summary = "Lấy danh sách tất cả câu hỏi trong kho")
    public ResponseEntity<ApiResponse<List<ChallengeBank>>> getAllChallenges() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi thành công", challengeBankService.getAllChallenges()));
    }

    @PostMapping("/quiz/{quizId}/challenges")
    @Operation(summary = "Gán câu hỏi vào quiz")
    public ResponseEntity<ApiResponse<List<QuizChallengeItem>>> assignChallengesToQuiz(
            @PathVariable UUID quizId,
            @RequestBody ChallengeAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Gán câu hỏi vào quiz thành công", 
                challengeBankService.assignChallengesToQuiz(quizId, request.getChallengeIds())));
    }
}
