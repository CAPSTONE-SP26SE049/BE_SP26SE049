package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.RegisterResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeBankRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.SpeakingAttempt;
import org.fsa_2026.company_fsa_captone_2026.service.AdminService;
import org.fsa_2026.company_fsa_captone_2026.service.ChallengeBankService;
import org.fsa_2026.company_fsa_captone_2026.service.SpeakingAttemptService;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserManagementResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserStatusUpdateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.UserUpdateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.UserAnalyticsResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.AnalyticsOverviewResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.SystemHealthResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.RewardResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.SpeakingAttemptLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin Controller
 * Handles admin-specific operations like creating educator accounts
 * Base: /api/v1/admin
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin API endpoints")
public class AdminController {

    private final AdminService adminService;
    private final ChallengeBankService challengeBankService;
    private final org.fsa_2026.company_fsa_captone_2026.service.QuizService quizService;
    private final SpeakingAttemptService speakingAttemptService;
    private final org.fsa_2026.company_fsa_captone_2026.service.EntryTestService entryTestService;
    private final org.fsa_2026.company_fsa_captone_2026.service.SystemConfigService systemConfigService;

    /**
     * Create Educator Account - POST /api/v1/admin/educators
     * Only Admin can create educator accounts
     */
    @PostMapping("/educators")
    @Operation(summary = "Create Educator Account", description = "Create a new educator account and send the generated password via email")
    public ResponseEntity<ApiResponse<RegisterResponse>> createEducatorAccount(
            @Valid @RequestBody EducatorCreateRequest request) {

        log.info("Admin attempting to create educator account for email: {}", request.getEmail());

        RegisterResponse response = adminService.createEducatorAccount(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tài khoản Educator thành công. Mật khẩu đã được gửi qua email.",
                        response));
    }

    /**
     * Create User Account (with role selection) - POST /api/v1/admin/users
     */
    @PostMapping("/users")
    @Operation(summary = "Create User Account", description = "Create a new user or educator account with role selection")
    public ResponseEntity<ApiResponse<RegisterResponse>> createUser(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String fullName = request.get("fullName");
        String role = request.getOrDefault("role", "USER");

        log.info("Admin creating {} account for email: {}", role, email);

        RegisterResponse response = adminService.createUserWithRole(email, fullName, role);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tài khoản thành công. Mật khẩu đã được gửi qua email.", response));
    }

    // ==========================================
    // 1. User Management APIs
    // ==========================================

    @GetMapping("/users")
    @Operation(summary = "Get All Users", description = "Retrieves a list of all users and educators")
    public ResponseEntity<ApiResponse<List<UserManagementResponse>>> getAllUsers() {
        log.info("Admin retrieving all users");
        List<UserManagementResponse> responses = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", responses));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get User Details", description = "Get details of a specific user by ID")
    public ResponseEntity<ApiResponse<UserManagementResponse>> getUserById(@PathVariable(name = "id") UUID id) {
        log.info("Admin retrieving user ID: {}", id);
        UserManagementResponse response = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", response));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Update User Status", description = "Lock or unlock a user account")
    public ResponseEntity<ApiResponse<UserManagementResponse>> updateUserStatus(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        log.info("Admin updating status for user ID: {} to active: {}", id, request.getIsActive());
        UserManagementResponse response = adminService.updateUserStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái người dùng thành công", response));
    }

    @PatchMapping("/users/{id}")
    @Operation(summary = "Update User Info", description = "Partially update user information (e.g. name, phone)")
    public ResponseEntity<ApiResponse<UserManagementResponse>> updateUser(
            @PathVariable(name = "id") UUID id,
            @RequestBody UserUpdateRequest request) {
        log.info("Admin updating information for user ID: {}", id);
        UserManagementResponse response = adminService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Chỉnh sửa thông tin người dùng thành công", response));
    }

    // ==========================================
    // 2. Content Management APIs
    // ==========================================

    @GetMapping("/content/challenges")
    @Operation(summary = "Get All Challenges", description = "Retrieves a list of all challenges")
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getAllChallenges() {
        log.info("Admin retrieving all challenges");
        List<ChallengeResponse> responses = adminService.getAllChallenges();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thử thách thành công", responses));
    }

    @GetMapping("/content/challenges/{id}")
    @Operation(summary = "Get Challenge Detail", description = "Get details of a specific challenge by ID")
    public ResponseEntity<ApiResponse<ChallengeResponse>> getChallengeById(@PathVariable(name = "id") UUID id) {
        log.info("Admin retrieving challenge detail ID: {}", id);
        ChallengeResponse response = adminService.getChallengeById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thử thách thành công", response));
    }

    @PostMapping("/content/challenges")
    @Operation(summary = "Create Challenge", description = "Create a new pronunciation challenge")
    public ResponseEntity<ApiResponse<ChallengeResponse>> createChallenge(
            @Valid @RequestBody ChallengeCreateRequest request) {
        log.info("Admin creating a new challenge");
        ChallengeResponse response = adminService.createChallenge(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thử thách thành công", response));
    }

    @PutMapping("/content/challenges/{id}")
    @Operation(summary = "Update Challenge", description = "Update an existing challenge by ID")
    public ResponseEntity<ApiResponse<ChallengeResponse>> updateChallenge(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody ChallengeCreateRequest request) {
        log.info("Admin updating challenge ID: {}", id);
        ChallengeResponse response = adminService.updateChallenge(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thử thách thành công", response));
    }

    @DeleteMapping("/content/challenges/{id}")
    @Operation(summary = "Delete Challenge (DEPRECATED)", description = "This operation has been moved to Educator role")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(@PathVariable(name = "id") UUID id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>builder()
                        .status("error")
                        .message("Quyền xóa thử thách đã được chuyển sang Educator")
                        .build());
    }

    // ==========================================
    // 1c. Content Management: Dialects
    // ==========================================

    // ==========================================
    // 1c. Content Management: Levels
    // ==========================================

    @GetMapping("/content/levels")
    @Operation(summary = "Get All Levels", description = "Retrieves a list of all levels")
    public ResponseEntity<ApiResponse<List<LevelResponse>>> getAllLevels() {
        log.info("Admin retrieving all levels");
        List<LevelResponse> responses = adminService.getAllLevels(); // Assuming this exists or needed
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cấp độ thành công", responses));
    }

    @GetMapping("/content/error-tags")
    @Operation(summary = "Get All Error Tags", description = "Retrieves a list of all error tags from learning_unit table")
    public ResponseEntity<ApiResponse<List<org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit>>> getErrorTags() {
        log.info("Admin retrieving all error tags");
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách Error Tags thành công", adminService.getErrorTags()));
    }

    @GetMapping("/content/levels/{id}")
    @Operation(summary = "Get Level Detail", description = "Get details of a specific level by ID")
    public ResponseEntity<ApiResponse<LevelResponse>> getLevelById(@PathVariable(name = "id") UUID id) {
        log.info("Admin retrieving level detail ID: {}", id);
        LevelResponse response = adminService.getLevelById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cấp độ thành công", response));
    }

    @PostMapping("/content/levels")
    @Operation(summary = "Create Level", description = "Create a new learning level")
    public ResponseEntity<ApiResponse<LevelResponse>> createLevel(
            @Valid @RequestBody LevelCreateRequest request) {
        log.info("Admin creating a new level: {}", request.getName());
        LevelResponse response = adminService.createLevel(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo cấp độ thành công", response));
    }

    @PutMapping("/content/levels/{id}")
    @Operation(summary = "Update Level", description = "Update an existing level by ID")
    public ResponseEntity<ApiResponse<LevelResponse>> updateLevel(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody LevelCreateRequest request) {
        log.info("Admin updating level ID: {}", id);
        LevelResponse response = adminService.updateLevel(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấp độ thành công", response));
    }

    @DeleteMapping("/content/levels/{id}")
    @Operation(summary = "Delete Level", description = "Delete a level by ID")
    public ResponseEntity<ApiResponse<Void>> deleteLevel(@PathVariable(name = "id") UUID id) {
        log.info("Admin deleting level ID: {}", id);
        adminService.deleteLevel(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa cấp độ thành công", null));
    }

    // ==========================================
    // 1d. Content Management: Challenge Bank
    // ==========================================

    @GetMapping("/content/challenge-bank")
    @Operation(summary = "Get All Challenges from Bank", description = "Retrieves a list of all challenges in the question bank")
    public ResponseEntity<ApiResponse<List<ChallengeBank>>> getAllChallengeBank() {
        log.info("Admin retrieving all challenge bank items");
        List<ChallengeBank> responses = challengeBankService.getAllChallenges();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi thành công", responses));
    }

    @PostMapping("/content/challenge-bank")
    @Operation(summary = "Create Challenge Bank Item", description = "Create a new challenge in the question bank")
    public ResponseEntity<ApiResponse<ChallengeBank>> createChallengeBankItem(
            @Valid @RequestBody ChallengeBankRequest request) {
        log.info("Admin creating a new challenge bank item");
        ChallengeBank response = challengeBankService.createChallenge(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo câu hỏi thành công", response));
    }

    @PutMapping("/content/challenge-bank/{id}")
    @Operation(summary = "Update Challenge Bank Item", description = "Update an existing challenge bank item by ID")
    public ResponseEntity<ApiResponse<ChallengeBank>> updateChallengeBankItem(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody ChallengeBankRequest request) {
        log.info("Admin updating challenge bank item ID: {}", id);
        ChallengeBank response = challengeBankService.updateChallenge(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật câu hỏi thành công", response));
    }

    @DeleteMapping("/content/challenge-bank/{id}")
    @Operation(summary = "Delete Challenge Bank Item", description = "Delete a challenge bank item by ID")
    public ResponseEntity<ApiResponse<Void>> deleteChallengeBankItem(@PathVariable(name = "id") UUID id) {
        log.info("Admin deleting challenge bank item ID: {}", id);
        challengeBankService.deleteChallenge(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa câu hỏi thành công", null));
    }

    @GetMapping("/content/{id}/history")
    @Operation(summary = "Get Content Approval History", description = "View the audit log for a specific level or challenge")
    public ResponseEntity<ApiResponse<List<org.fsa_2026.company_fsa_captone_2026.dto.ContentApprovalHistoryResponse>>> getContentApprovalHistory(
            @PathVariable(name = "id") UUID id) {
        log.info("Admin retrieving approval history for content ID: {}", id);
        List<org.fsa_2026.company_fsa_captone_2026.dto.ContentApprovalHistoryResponse> responses = adminService
                .getContentApprovalHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử duyệt thành công", responses));
    }

    // ==========================================
    // 1e. Content Management: Rewards/Achievements
    // ==========================================

    @GetMapping("/rewards")
    @Operation(summary = "Get All Rewards", description = "Retrieves a list of all rewards/badges for admin")
    public ResponseEntity<ApiResponse<List<RewardResponse>>> getAllRewards() {
        log.info("Admin retrieving all rewards");
        List<RewardResponse> responses = adminService.getAllRewards();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành tựu thành công", responses));
    }

    @GetMapping("/rewards/{id}")
    @Operation(summary = "Get Reward by ID", description = "Get details of a specific reward/badge by ID")
    public ResponseEntity<ApiResponse<RewardResponse>> getRewardById(@PathVariable(name = "id") UUID id) {
        log.info("Admin retrieving reward detail ID: {}", id);
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin thành tựu thành công", adminService.getRewardById(id)));
    }

    @PostMapping("/rewards")
    @Operation(summary = "Create Reward", description = "Create a new reward/badge")
    public ResponseEntity<ApiResponse<RewardResponse>> createReward(
            @Valid @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.RewardCreateRequest request) {
        log.info("Admin creating a new reward: {}", request.getCode());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thành tựu thành công", adminService.createReward(request)));
    }

    @PutMapping("/rewards/{id}")
    @Operation(summary = "Update Reward", description = "Update an existing reward/badge by ID")
    public ResponseEntity<ApiResponse<RewardResponse>> updateReward(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.RewardCreateRequest request) {
        log.info("Admin updating reward ID: {}", id);
        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật thành tựu thành công", adminService.updateReward(id, request)));
    }

    @DeleteMapping("/rewards/{id}")
    @Operation(summary = "Delete Reward", description = "Delete a reward/badge by ID")
    public ResponseEntity<ApiResponse<Void>> deleteReward(@PathVariable(name = "id") UUID id) {
        log.info("Admin deleting reward ID: {}", id);
        adminService.deleteReward(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành tựu thành công", null));
    }

    @PatchMapping("/rewards/{id}/toggle")
    @Operation(summary = "Toggle Reward Status", description = "Turn a reward active status on or off")
    public ResponseEntity<ApiResponse<RewardResponse>> toggleReward(@PathVariable(name = "id") UUID id) {
        log.info("Admin toggling reward status ID: {}", id);
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật trạng thái thành tựu thành công", adminService.toggleRewardActive(id)));
    }

    @PostMapping("/rewards/{rewardId}/attach/{quizId}")
    @Operation(summary = "Attach Reward to Quiz", description = "Link a reward/badge to a specific quiz")
    public ResponseEntity<ApiResponse<Void>> attachRewardToQuiz(
            @PathVariable(name = "rewardId") UUID rewardId,
            @PathVariable(name = "quizId") UUID quizId) {
        log.info("Admin attaching reward ID: {} to quiz ID: {}", rewardId, quizId);
        adminService.attachRewardToQuiz(rewardId, quizId);
        return ResponseEntity.ok(ApiResponse.success("Gán thành tựu cho quiz thành công", null));
    }

    // ==========================================
    // 2. User Analytics APIs
    // ==========================================

    @GetMapping("/analytics/overview")
    @Operation(summary = "Analytics Overview", description = "Get aggregate user statistics from DB")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getAnalyticsOverview() {
        log.info("Admin requesting analytics overview");
        AnalyticsOverviewResponse response = adminService.getAnalyticsOverview();
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    @GetMapping("/analytics/users-progress")
    @Operation(summary = "Users Progress Analytics", description = "Get detailed progress for all users (exclude admin/educator)")
    public ResponseEntity<ApiResponse<List<UserAnalyticsResponse>>> getUsersProgress() {
        log.info("Admin requesting users progress analytics");
        List<UserAnalyticsResponse> responses = adminService.getUsersAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Thành công", responses));
    }

    @GetMapping("/ai-monitor/logs")
    @Operation(summary = "AI Monitor Logs", description = "Get recent speaking attempt logs with latency and feedback details")
    public ResponseEntity<ApiResponse<List<SpeakingAttemptLogResponse>>> getAiMonitorLogs(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        log.info("Admin requesting AI monitor logs, limit={}", limit);
        List<SpeakingAttemptLogResponse> logs = speakingAttemptService.getAiMonitorLogs(limit);
        return ResponseEntity.ok(ApiResponse.success("Thành công", logs));
    }

    @GetMapping("/analytics/engagement")
    @Operation(summary = "User Engagement", description = "Get engagement metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsEngagement() {
        log.info("Admin requesting engagement metrics");
        Map<String, Object> response = adminService.getRealEngagementMetrics();
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    @GetMapping("/analytics/errors/heatmaps")
    @Operation(summary = "Error Heatmaps", description = "Get error pattern heat maps across regions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getErrorHeatmaps() {
        log.info("Admin requesting error heatmaps");
        Map<String, Object> response = adminService.getErrorHeatmaps();
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    // ==========================================
    // 3. System Monitoring APIs (Mock Data)
    // ==========================================

    @GetMapping("/system/health")
    @Operation(summary = "System Health", description = "Get server and database status")
    public ResponseEntity<ApiResponse<SystemHealthResponse>> getSystemHealth() {
        log.info("Admin requesting system health");
        SystemHealthResponse mockResponse = SystemHealthResponse.builder()
                .status("UP")
                .databaseStatus("CONNECTED")
                .parakeetStatus("ONLINE")
                .groqStatus("CONNECTED")
                .uptimeSeconds(86400)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Thành công", mockResponse));
    }

    @GetMapping("/system/ai-performance")
    @Operation(summary = "AI Performance", description = "Metrics for AI model latency and accuracy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiPerformance() {
        log.info("Admin requesting AI performance metrics");
        Map<String, Object> response = adminService.getAiPerformance();
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    @GetMapping("/system/feedback")
    @Operation(summary = "User Feedback", description = "Retrieve user-submitted feedback logs")
    public ResponseEntity<ApiResponse<List<String>>> getSystemFeedback() {
        log.info("Admin requesting system feedback");
        List<String> mockResponse = List.of(
                "App is great but sometimes audio is lagging.",
                "Need more southern dialect practice words.");
        return ResponseEntity.ok(ApiResponse.success("Thành công", mockResponse));
    }

    // ==========================================
    // 4. Quiz Management APIs
    // ==========================================

    @GetMapping("/content/quizzes")
    @Operation(summary = "Get All Quizzes", description = "Fetch all quizzes, optionally filtered by levelId")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuizzes(
            @RequestParam(value = "levelId", required = false) UUID levelId) {
        log.info("Admin retrieving quizzes. Level filter: {}", levelId);
        List<Map<String, Object>> responses = levelId != null
                ? quizService.getQuizzesByLevel(levelId)
                : quizService.getAllQuizzes();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quiz thành công", responses));
    }

    @GetMapping("/content/quizzes/{id}")
    @Operation(summary = "Get Quiz Detail", description = "Get details of a specific quiz by ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuizById(@PathVariable(name = "id") UUID id) {
        log.info("Admin retrieving quiz detail ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin quiz thành công", quizService.getQuizDetails(id)));
    }

    @PostMapping("/content/quizzes")
    @Operation(summary = "Create Quiz", description = "Create a new quiz stored in LearningUnit")
    public ResponseEntity<ApiResponse<org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit>> createQuiz(
            @Valid @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.QuizCreateRequest request) {
        log.info("Admin creating a new quiz: {}", request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo quiz thành công", quizService.createQuiz(request)));
    }

    @PutMapping("/content/quizzes/{id}")
    @Operation(summary = "Update Quiz", description = "Update an existing quiz by ID")
    public ResponseEntity<ApiResponse<org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit>> updateQuiz(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.QuizCreateRequest request) {
        log.info("Admin updating quiz ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quiz thành công", quizService.updateQuiz(id, request)));
    }

    @DeleteMapping("/content/quizzes/{id}")
    @Operation(summary = "Delete Quiz", description = "Delete a quiz by ID")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable(name = "id") UUID id) {
        log.info("Admin deleting quiz ID: {}", id);
        quizService.deleteQuiz(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa quiz thành công", null));
    }

    @PutMapping("/content/quizzes/reorder")
    @Operation(summary = "Reorder Quizzes", description = "Update orderIndex for a list of quizzes")
    public ResponseEntity<ApiResponse<Void>> reorderQuizzes(@RequestBody List<UUID> quizIds) {
        log.info("Admin reordering {} quizzes", quizIds.size());
        quizService.reorderQuizzes(quizIds);
        return ResponseEntity.ok(ApiResponse.success("Thay đổi thứ tự bài tập thành công", null));
    }

    @GetMapping("/content/quizzes/{id}/challenges")
    @Operation(summary = "Get Quiz Challenges", description = "Get challenges assigned to a quiz")
    public ResponseEntity<ApiResponse<List<org.fsa_2026.company_fsa_captone_2026.dto.QuizChallengeItemResponse>>> getQuizChallenges(
            @PathVariable(name = "id") UUID id) {
        log.info("Admin retrieving challenges for quiz ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thử thách của quiz thành công",
                challengeBankService.getChallengesByQuizId(id)));
    }

    @PostMapping("/content/quizzes/{id}/challenges")
    @Operation(summary = "Assign Challenges to Quiz", description = "Assign challenges to a quiz and return updated scoring")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignChallengesToQuiz(
            @PathVariable(name = "id") UUID id,
            @RequestBody Map<String, List<UUID>> request) {
        List<UUID> challengeIds = request.get("challengeIds");
        log.info("Admin assigning challenges to quiz ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Gán thử thách vào quiz thành công",
                challengeBankService.assignChallengesToQuiz(id, challengeIds)));
    }

    @DeleteMapping("/content/quizzes/{id}/challenges/{challengeId}")
    @Operation(summary = "Remove Challenge from Quiz", description = "Remove a challenge from a quiz and return updated scoring")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeChallengeFromQuiz(
            @PathVariable(name = "id") UUID id,
            @PathVariable(name = "challengeId") UUID challengeId) {
        log.info("Admin removing challenge ID: {} from quiz ID: {}", challengeId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thử thách khỏi quiz thành công",
                challengeBankService.removeChallengeFromQuiz(id, challengeId)));
    }

    // ==========================================
    // 5. Speaking Dataset APIs (Admin Only)
    // ==========================================

    @GetMapping("/dataset/speaking")
    @Operation(summary = "Get Speaking Attempts", description = "Get all speaking attempts for dataset review. Filter by dialect (NORTH, CENTRAL, SOUTH)")
    public ResponseEntity<ApiResponse<Page<SpeakingAttempt>>> getSpeakingAttempts(
            @RequestParam(name = "dialect", required = false) String dialect,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        log.info("Admin retrieving speaking attempts. Dialect: {}, Page: {}, Size: {}", dialect, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<SpeakingAttempt> result = speakingAttemptService.getAttempts(dialect, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách mẫu giọng nói thành công", result));
    }

    @GetMapping("/dataset/speaking/stats")
    @Operation(summary = "Get Speaking Dataset Stats", description = "Get total count of collected voice samples, grouped by dialect")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getSpeakingStats() {
        log.info("Admin retrieving speaking dataset stats");
        Map<String, Long> stats = speakingAttemptService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Thống kê dataset giọng nói thành công", stats));
    }

    // ==========================================
    // 6. Entry Test Question CRUD
    // ==========================================

    @GetMapping("/content/entry-test-questions")
    @Operation(summary = "Get All Entry Test Questions", description = "Retrieves all questions for the entry test")
    public ResponseEntity<ApiResponse<List<org.fsa_2026.company_fsa_captone_2026.dto.EntryTestQuestionResponse>>> getAllEntryTestQuestions() {
        log.info("Admin retrieving all entry test questions");
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách câu hỏi entry test thành công",
                entryTestService.getAllQuestions()));
    }

    @PostMapping("/content/entry-test-questions")
    @Operation(summary = "Create Entry Test Question", description = "Create a new entry test question")
    public ResponseEntity<ApiResponse<org.fsa_2026.company_fsa_captone_2026.dto.EntryTestQuestionResponse>> createEntryTestQuestion(
            @Valid @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.EntryTestQuestionRequest request) {
        log.info("Admin creating entry test question: {}", request.getTargetText());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo câu hỏi entry test thành công",
                        entryTestService.createQuestion(request)));
    }

    @PutMapping("/content/entry-test-questions/{id}")
    @Operation(summary = "Update Entry Test Question", description = "Update an existing entry test question")
    public ResponseEntity<ApiResponse<org.fsa_2026.company_fsa_captone_2026.dto.EntryTestQuestionResponse>> updateEntryTestQuestion(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.EntryTestQuestionRequest request) {
        log.info("Admin updating entry test question ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật câu hỏi entry test thành công",
                entryTestService.updateQuestion(id, request)));
    }

    @DeleteMapping("/content/entry-test-questions/{id}")
    @Operation(summary = "Delete Entry Test Question", description = "Delete an entry test question")
    public ResponseEntity<ApiResponse<Void>> deleteEntryTestQuestion(@PathVariable(name = "id") UUID id) {
        log.info("Admin deleting entry test question ID: {}", id);
        entryTestService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa câu hỏi entry test thành công", null));
    }

    // ==========================================
    // 7. System AI Configurations Management
    // ==========================================

    @GetMapping("/configs")
    @Operation(summary = "Get All AI and System Configurations", description = "Retrieves all system configuration parameters")
    public ResponseEntity<ApiResponse<List<org.fsa_2026.company_fsa_captone_2026.entity.SystemConfig>>> getAllConfigs() {
        log.info("Admin retrieving all system configurations");
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cấu hình hệ thống thành công",
                systemConfigService.getAllConfigs()));
    }

    @PutMapping("/configs")
    @Operation(summary = "Bulk Update AI and System Configurations", description = "Bulk updates multiple system configurations at once")
    public ResponseEntity<ApiResponse<Void>> updateConfigs(@RequestBody Map<String, String> configMap) {
        log.info("Admin updating {} system configurations", configMap.size());
        systemConfigService.updateConfigs(configMap);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình hệ thống thành công", null));
    }

}
