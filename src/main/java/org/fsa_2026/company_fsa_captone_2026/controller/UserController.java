package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.AccountBadgeResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelProgressResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizChallengeItemResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCompleteRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCompleteResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserProfileResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserRegionProgressResponse;
import org.fsa_2026.company_fsa_captone_2026.service.AuthService;
import org.fsa_2026.company_fsa_captone_2026.service.ChallengeBankService;
import org.fsa_2026.company_fsa_captone_2026.service.QuizService;
import org.fsa_2026.company_fsa_captone_2026.service.UserProfileService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
        private final org.fsa_2026.company_fsa_captone_2026.service.FeedbackService feedbackService;

        @GetMapping("/mailbox")
        @Operation(summary = "Get feedback mailbox", description = "Lấy hòm thư phản hồi từ giáo viên cho học viên hiện tại")
        public ResponseEntity<ApiResponse<List<org.fsa_2026.company_fsa_captone_2026.dto.FeedbackDtos.FeedbackResponse>>> getMailbox(
                        Authentication authentication) {
                log.info("User {} lấy hòm thư phản hồi", authentication.getName());
                return ResponseEntity
                                .ok(ApiResponse.success(feedbackService.getLearnerMailbox(authentication.getName())));
        }

        /**
         * Get Current User Profile - GET /api/v1/users/me
         */
        @GetMapping("/me")
        @Operation(summary = "Get User Profile", description = "Retrieve the currently authenticated user's profile", security = @SecurityRequirement(name = "bearer-jwt"))
        public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile(Authentication authentication) {
                log.info("Get profile for user: {}", authentication.getName());
                UserProfileResponse profile = authService.getUserProfile(authentication.getName());
                return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cá nhân thành công", profile));
        }

        /**
         * Update Current User Profile - PUT /api/v1/users/me
         */
        @PutMapping("/me")
        @Operation(summary = "Update User Profile", description = "Update the currently authenticated user's profile information", security = @SecurityRequirement(name = "bearer-jwt"))
        public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUserProfile(
                        Authentication authentication,
                        @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.UserProfileRequest request) {
                log.info("Update profile for user: {}", authentication.getName());
                UserProfileResponse updatedProfile = userProfileService.updateProfile(authentication.getName(),
                                request);
                return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", updatedProfile));
        }

        @GetMapping("/levels/{levelId}/quizzes")
        @Operation(summary = "Get quizzes in level for user", description = "Lấy danh sách quiz bên trong chương theo levelId để user chọn màn chơi", security = @SecurityRequirement(name = "bearer-jwt"))
        public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuizzesByLevelForUser(
                        @PathVariable UUID levelId,
                        Authentication authentication) {

                log.info("User {} lấy danh sách quiz theo levelId: {}", authentication.getName(), levelId);

                List<Map<String, Object>> quizzes = quizService.getQuizzesByLevel(levelId);
                return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quiz theo chương thành công", quizzes));
        }

        @GetMapping("/challenges/{challengeId}/quizzes")
        @Operation(summary = "Get quizzes by challenge for user", description = "Lấy danh sách quiz đã được gán câu hỏi theo challengeId trong nhóm User API", security = @SecurityRequirement(name = "bearer-jwt"))
        public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuizzesByChallengeForUser(
                        @PathVariable UUID challengeId,
                        Authentication authentication) {

                log.info("User {} lấy danh sách quiz theo challengeId: {}", authentication.getName(), challengeId);

                List<Map<String, Object>> quizzes = quizService.getQuizzesByChallengeId(challengeId);
                return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quiz theo câu hỏi thành công", quizzes));
        }

        @GetMapping("/quizzes/{quizId}/challenges")
        @Operation(summary = "Get assigned questions in quiz for user", description = "Lấy toàn bộ câu hỏi đã được gán vào quiz theo quizId trong nhóm User API", security = @SecurityRequirement(name = "bearer-jwt"))
        public ResponseEntity<ApiResponse<List<QuizChallengeItemResponse>>> getChallengesByQuizForUser(
                        @PathVariable UUID quizId,
                        Authentication authentication) {

                log.info("User {} lấy danh sách câu hỏi theo quizId: {}", authentication.getName(), quizId);

                List<QuizChallengeItemResponse> challenges = challengeBankService.getChallengesByQuizId(quizId);
                return ResponseEntity
                                .ok(ApiResponse.success("Lấy danh sách câu hỏi trong quiz thành công", challenges));
        }

        @GetMapping("/quizzes/{quizId}")
        @Operation(summary = "Get quiz details for user", description = "Lấy thông tin chi tiết quiz (tên, mô tả, thời gian, điểm sàn...) cho User", security = @SecurityRequirement(name = "bearer-jwt"))
        public ResponseEntity<ApiResponse<Map<String, Object>>> getQuizDetailsForUser(
                        @PathVariable UUID quizId,
                        Authentication authentication) {

                log.info("User {} lấy thông tin chi tiết quiz: {}", authentication.getName(), quizId);

                Map<String, Object> quiz = quizService.getQuizDetails(quizId);
                return ResponseEntity.ok(ApiResponse.success("Lấy thông tin quiz thành công", quiz));
        }

        // ==========================================
        // Quiz Completion & Progress
        // ==========================================

        /**
         * POST /api/v1/users/quizzes/{quizId}/complete
         * Người chơi gửi kết quả quiz → backend tự động trao thành tựu nếu đạt.
         */
        @PostMapping("/quizzes/{quizId}/complete")
        @Operation(summary = "Complete Quiz", description = "Submit quiz results. Auto-grants the attached reward if score >= passing_score.", security = @SecurityRequirement(name = "bearer-jwt"))
        public ResponseEntity<ApiResponse<QuizCompleteResponse>> completeQuiz(
                        @PathVariable UUID quizId,
                        @Valid @RequestBody QuizCompleteRequest request,
                        Authentication authentication) {
                log.info("User {} completing quiz {}", authentication.getName(), quizId);
                QuizCompleteResponse response = quizService.completeQuiz(quizId, request, authentication.getName());
                return ResponseEntity.ok(ApiResponse.success("Hoàn thành quiz", response));
        }

        /**
         * GET /api/v1/users/levels/{levelId}/progress
         * Xem tiến trình quiz trong 1 level: quiz nào đã hoàn thành, điểm, sao, thành
         * tựu.
         */
        @GetMapping("/levels/{levelId}/progress")
        @Operation(summary = "Get Level Progress", description = "View quiz progress within a level: which quizzes completed, scores, stars, rewards earned.", security = @SecurityRequirement(name = "bearer-jwt"))
        public ResponseEntity<ApiResponse<LevelProgressResponse>> getLevelProgress(
                        @PathVariable UUID levelId,
                        Authentication authentication) {
                log.info("User {} getting progress for level {}", authentication.getName(), levelId);
                LevelProgressResponse response = quizService.getLevelProgress(levelId, authentication.getName());
                return ResponseEntity.ok(ApiResponse.success("Lấy tiến trình level thành công", response));
        }

        /**
         * GET /api/v1/users/my-rewards
         * Xem tất cả thành tựu đã nhận của user hiện tại.
         */
        @GetMapping("/my-rewards")
        @Operation(summary = "Get My Rewards", description = "View all rewards/achievements earned by the current user.", security = @SecurityRequirement(name = "bearer-jwt"))
        public ResponseEntity<ApiResponse<List<AccountBadgeResponse>>> getMyRewards(
                        Authentication authentication) {
                log.info("User {} getting rewards", authentication.getName());
                return ResponseEntity.ok(ApiResponse.success("Lấy thành tựu thành công",
                                quizService.getUserRewards(authentication.getName())));
        }

        @GetMapping("/me/progress")
        public ResponseEntity<ApiResponse<UserRegionProgressResponse>> getMyProgress(Authentication authentication) {
                log.info("Getting progress for user: {}", authentication.getName());
                UserRegionProgressResponse progress = quizService.getMyProgressSummary(authentication.getName());
                return ResponseEntity.ok(ApiResponse.success("Lấy tiến trình thành công", progress));
        }
}
