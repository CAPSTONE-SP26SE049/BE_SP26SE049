package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeAssignRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeBankRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizChallengeItemResponse;
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
@Slf4j
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
    public ResponseEntity<ApiResponse<List<ChallengeBank>>> getAllChallenges(
            @RequestParam(required = false) String skillType,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) UUID levelId
    ) {
        List<ChallengeBank> challenges = challengeBankService.getAllChallenges(skillType, region, levelId);
        log.debug("challenge-bank filter: count={}, skillType={}, region={}, levelId={}",
                challenges.size(), skillType, region, levelId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi thành công", challenges));
    }

    @PostMapping("/quiz/{quizId}/challenges")
    @Operation(summary = "Gán câu hỏi vào quiz")
    public ResponseEntity<ApiResponse<List<QuizChallengeItem>>> assignChallengesToQuiz(
            @PathVariable UUID quizId,
            @RequestBody ChallengeAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Gán câu hỏi vào quiz thành công", 
                challengeBankService.assignChallengesToQuiz(quizId, request.getChallengeIds())));
    }
    @GetMapping("/quiz/{quizId}/challenges")
    @Operation(summary = "Lấy danh sách câu hỏi đã gán vào quiz")
    public ResponseEntity<ApiResponse<List<QuizChallengeItemResponse>>> getChallengesByQuizId(
            @PathVariable UUID quizId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi thành công",
                challengeBankService.getChallengesByQuizId(quizId)));
    }

    @PutMapping("/challenge-bank/{id}")
    @Operation(summary = "Cập nhật câu hỏi")
    public ResponseEntity<ApiResponse<ChallengeBank>> updateChallenge(
            @PathVariable UUID id,
            @Valid @RequestBody ChallengeBankRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật câu hỏi thành công", 
                challengeBankService.updateChallenge(id, request)));
    }

    @DeleteMapping("/challenge-bank/{id}")
    @Operation(summary = "Xóa câu hỏi khỏi kho (sẽ bị gỡ khỏi tất cả quiz)")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(@PathVariable UUID id) {
        challengeBankService.deleteChallenge(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa câu hỏi thành công", null));
    }

    @DeleteMapping("/quiz/{quizId}/challenges/{challengeId}")
    @Operation(summary = "Gỡ câu hỏi khỏi một quiz nhưng vẫn giữ trong kho")
    public ResponseEntity<ApiResponse<Void>> removeChallengeFromQuiz(
            @PathVariable UUID quizId,
            @PathVariable UUID challengeId) {
        challengeBankService.removeChallengeFromQuiz(quizId, challengeId);
        return ResponseEntity.ok(ApiResponse.success("Gỡ câu hỏi khỏi quiz thành công", null));
    }
}
