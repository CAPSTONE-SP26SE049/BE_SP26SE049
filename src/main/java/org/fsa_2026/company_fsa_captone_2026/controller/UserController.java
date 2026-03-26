package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizChallengeItemResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserProfileResponse;
import org.fsa_2026.company_fsa_captone_2026.service.AuthService;
import org.fsa_2026.company_fsa_captone_2026.service.ChallengeBankService;
import org.fsa_2026.company_fsa_captone_2026.service.QuizService;
import org.fsa_2026.company_fsa_captone_2026.service.UserProfileService;
import org.fsa_2026.company_fsa_captone_2026.repository.ContentItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * User Controller
 * Handles user profile operations and learner-facing query endpoints.
 * Base: /api/v1/users
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User API endpoints")
public class UserController {

    private final AuthService authService;
    private final UserProfileService userProfileService;
    private final QuizService quizService;
    private final ChallengeBankService challengeBankService;
    private final ContentItemRepository contentItemRepository;
    private final org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository learningUnitRepository;

    /**
     * Get Current User Profile - GET /api/v1/users/me
     */
    @GetMapping("/me")
    @Operation(summary = "Get User Profile", description = "Retrieve the currently authenticated user's profile",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile(Authentication authentication) {
        log.info("Get profile for user: {}", authentication.getName());
        UserProfileResponse profile = authService.getUserProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cá nhân thành công", profile));
    }

    /**
     * Update Current User Profile - PUT /api/v1/users/me
     */
    @PutMapping("/me")
    @Operation(summary = "Update User Profile", description = "Update the currently authenticated user's profile information",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUserProfile(
            Authentication authentication,
            @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.UserProfileRequest request) {
        log.info("Update profile for user: {}", authentication.getName());
        UserProfileResponse updatedProfile = userProfileService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", updatedProfile));
    }

    /**
     * Get quizzes in a level for user - GET /api/v1/users/levels/{levelId}/quizzes
     * Returns ContentItem type=QUIZ entities linked to the level (created by admin/educator)
     */
    @GetMapping("/levels/{levelId}/quizzes")
    @Operation(summary = "Get quizzes in level for user", description = "Get all quizzes belonging to a specific level",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getQuizzesByLevel(
            @PathVariable UUID levelId) {
        log.info("Get quizzes for levelId: {}", levelId);
        
        // 1. Quizzes from ContentItem (Modern system)
        List<QuizResponse> contentQuizzes = contentItemRepository
                .findByLearningUnitIdAndType(levelId, "QUIZ")
                .stream()
                .map(QuizResponse::fromEntity)
                .collect(java.util.stream.Collectors.toList());

        // 2. Quizzes from LearningUnit parent-child (Legacy/Educator system)
        List<QuizResponse> legacyQuizzes = learningUnitRepository
                .findByParentIdAndType(levelId, "QUIZ")
                .stream()
                .map(QuizResponse::fromLearningUnit)
                .collect(java.util.stream.Collectors.toList());

        // Aggregate
        List<QuizResponse> allQuizzes = new java.util.ArrayList<>();
        allQuizzes.addAll(contentQuizzes);
        allQuizzes.addAll(legacyQuizzes);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quiz theo level thành công", allQuizzes));
    }

    /**
     * Get quiz detail for user - GET /api/v1/users/quizzes/{quizId}
     * Reads ContentItem type=QUIZ
     */
    @GetMapping("/quizzes/{quizId}")
    @Operation(summary = "Get quiz detail for user", description = "Get quiz info (title, passingScore, timeLimitMinutes...)",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<QuizResponse>> getQuizDetail(
            @PathVariable UUID quizId) {
        log.info("Get quiz detail for quizId: {}", quizId);
        QuizResponse quiz = contentItemRepository.findById(quizId)
                .map(QuizResponse::fromEntity)
                .orElseThrow(() -> new org.fsa_2026.company_fsa_captone_2026.exception.ApiException("NOT_FOUND", "Không tìm thấy quiz"));
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin quiz thành công", quiz));
    }

    /**
     * Get assigned questions in quiz for user - GET /api/v1/users/quizzes/{quizId}/challenges
     */
    @GetMapping("/quizzes/{quizId}/challenges")
    @Operation(summary = "Get assigned questions in quiz for user", description = "Get all challenges/questions assigned to a specific quiz",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<QuizChallengeItemResponse>>> getChallengesByQuiz(
            @PathVariable UUID quizId) {
        log.info("Get challenges for quizId: {}", quizId);
        List<QuizChallengeItemResponse> challenges = challengeBankService.getChallengesByQuizId(quizId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi trong quiz thành công", challenges));
    }

    /**
     * Get quizzes by challenge for user - GET /api/v1/users/challenges/{challengeId}/quizzes
     */
    @GetMapping("/challenges/{challengeId}/quizzes")
    @Operation(summary = "Get quizzes by challenge for user", description = "Get all quizzes that contain a specific challenge",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuizzesByChallenge(
            @PathVariable UUID challengeId) {
        log.info("Get quizzes for challengeId: {}", challengeId);
        List<Map<String, Object>> quizzes = quizService.getQuizzesByChallenge(challengeId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quiz theo challenge thành công", quizzes));
    }
}
