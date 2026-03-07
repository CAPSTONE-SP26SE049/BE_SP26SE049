package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.RegisterResponse;
import org.fsa_2026.company_fsa_captone_2026.service.AdminService;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserManagementResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserStatusUpdateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.UserUpdateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.AnalyticsOverviewResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.SystemHealthResponse;
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
    public ResponseEntity<ApiResponse<UserManagementResponse>> getUserById(@PathVariable UUID id) {
        log.info("Admin retrieving user ID: {}", id);
        UserManagementResponse response = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", response));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Update User Status", description = "Lock or unlock a user account")
    public ResponseEntity<ApiResponse<UserManagementResponse>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        log.info("Admin updating status for user ID: {} to active: {}", id, request.getIsActive());
        UserManagementResponse response = adminService.updateUserStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái người dùng thành công", response));
    }

    @PatchMapping("/users/{id}")
    @Operation(summary = "Update User Info", description = "Partially update user information (e.g. name, phone)")
    public ResponseEntity<ApiResponse<UserManagementResponse>> updateUser(
            @PathVariable UUID id,
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
    public ResponseEntity<ApiResponse<ChallengeResponse>> getChallengeById(@PathVariable UUID id) {
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
            @PathVariable UUID id,
            @Valid @RequestBody ChallengeCreateRequest request) {
        log.info("Admin updating challenge ID: {}", id);
        ChallengeResponse response = adminService.updateChallenge(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thử thách thành công", response));
    }

    @DeleteMapping("/content/challenges/{id}")
    @Operation(summary = "Delete Challenge (DEPRECATED)", description = "This operation has been moved to Educator role")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>builder()
                        .status("error")
                        .message("Quyền xóa thử thách đã được chuyển sang Educator")
                        .build());
    }

    // ==========================================
    // 1b. Content Management: Pending & Approvals
    // ==========================================

    @GetMapping("/content/pending/levels")
    @Operation(summary = "Get Pending Levels", description = "Retrieves a list of all levels awaiting admin approval")
    public ResponseEntity<ApiResponse<List<LevelResponse>>> getPendingLevels() {
        log.info("Admin retrieving pending levels");
        List<LevelResponse> responses = adminService.getPendingLevels();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài học chờ duyệt thành công", responses));
    }

    @GetMapping("/content/pending/challenges")
    @Operation(summary = "Get Pending Challenges", description = "Retrieves a list of all challenges awaiting admin approval")
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> getPendingChallenges() {
        log.info("Admin retrieving pending challenges");
        List<ChallengeResponse> responses = adminService.getPendingChallenges();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài tập chờ duyệt thành công", responses));
    }

    @PutMapping("/content/levels/{id}/review")
    @Operation(summary = "Review Level", description = "Approve or Reject a pending level")
    public ResponseEntity<ApiResponse<LevelResponse>> reviewLevel(
            @PathVariable UUID id,
            @Valid @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.ContentReviewRequest request) {
        log.info("Admin reviewing level ID: {} with status: {}", id, request.getStatus());
        LevelResponse response = adminService.reviewLevel(id, request);
        return ResponseEntity.ok(ApiResponse.success("Duyệt bài học thành công", response));
    }

    @PutMapping("/content/challenges/{id}/review")
    @Operation(summary = "Review Challenge", description = "Approve or Reject a pending challenge")
    public ResponseEntity<ApiResponse<ChallengeResponse>> reviewChallenge(
            @PathVariable UUID id,
            @Valid @RequestBody org.fsa_2026.company_fsa_captone_2026.dto.ContentReviewRequest request) {
        log.info("Admin reviewing challenge ID: {} with status: {}", id, request.getStatus());
        ChallengeResponse response = adminService.reviewChallenge(id, request);
        return ResponseEntity.ok(ApiResponse.success("Duyệt bài tập thành công", response));
    }

    // ==========================================
    // 1c. Content Management: Dialects
    // ==========================================

    @PostMapping("/content/dialects")
    @Operation(summary = "Create Dialect", description = "Create a new regional dialect")
    public ResponseEntity<ApiResponse<DialectResponse>> createDialect(
            @Valid @RequestBody DialectCreateRequest request) {
        log.info("Admin creating a new dialect: {}", request.getName());
        DialectResponse response = adminService.createDialect(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo vùng miền phát âm thành công", response));
    }

    @PutMapping("/content/dialects/{id}")
    @Operation(summary = "Update Dialect", description = "Update an existing dialect by ID")
    public ResponseEntity<ApiResponse<DialectResponse>> updateDialect(
            @PathVariable UUID id,
            @Valid @RequestBody DialectCreateRequest request) {
        log.info("Admin updating dialect ID: {}", id);
        DialectResponse response = adminService.updateDialect(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vùng miền thành công", response));
    }

    @DeleteMapping("/content/dialects/{id}")
    @Operation(summary = "Delete Dialect", description = "Delete a dialect by ID")
    public ResponseEntity<ApiResponse<Void>> deleteDialect(@PathVariable UUID id) {
        log.info("Admin deleting dialect ID: {}", id);
        adminService.deleteDialect(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa vùng miền thành công", null));
    }

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

    @GetMapping("/content/levels/{id}")
    @Operation(summary = "Get Level Detail", description = "Get details of a specific level by ID")
    public ResponseEntity<ApiResponse<LevelResponse>> getLevelById(@PathVariable UUID id) {
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
            @PathVariable UUID id,
            @Valid @RequestBody LevelCreateRequest request) {
        log.info("Admin updating level ID: {}", id);
        LevelResponse response = adminService.updateLevel(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấp độ thành công", response));
    }

    @DeleteMapping("/content/levels/{id}")
    @Operation(summary = "Delete Level", description = "Delete a level by ID")
    public ResponseEntity<ApiResponse<Void>> deleteLevel(@PathVariable UUID id) {
        log.info("Admin deleting level ID: {}", id);
        adminService.deleteLevel(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa cấp độ thành công", null));
    }

    @GetMapping("/content/{id}/history")
    @Operation(summary = "Get Content Approval History", description = "View the audit log for a specific level or challenge")
    public ResponseEntity<ApiResponse<List<org.fsa_2026.company_fsa_captone_2026.dto.ContentApprovalHistoryResponse>>> getContentApprovalHistory(
            @PathVariable UUID id) {
        log.info("Admin retrieving approval history for content ID: {}", id);
        List<org.fsa_2026.company_fsa_captone_2026.dto.ContentApprovalHistoryResponse> responses = adminService
                .getContentApprovalHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử duyệt thành công", responses));
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

    @GetMapping("/analytics/engagement")
    @Operation(summary = "User Engagement", description = "Get engagement metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsEngagement() {
        log.info("Admin requesting engagement metrics");
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("dailyActiveUsers", 120);
        mockResponse.put("averageSessionTimeMinutes", 15.5);
        return ResponseEntity.ok(ApiResponse.success("Thành công", mockResponse));
    }

    @GetMapping("/analytics/errors/heatmaps")
    @Operation(summary = "Error Heatmaps", description = "Get error pattern heat maps across regions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getErrorHeatmaps() {
        log.info("Admin requesting error heatmaps");
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("northern_N_L_confusion_rate", 0.45);
        mockResponse.put("central_S_X_confusion_rate", 0.38);
        return ResponseEntity.ok(ApiResponse.success("Thành công", mockResponse));
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
                .aiModelStatus("ONLINE")
                .uptimeSeconds(86400)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Thành công", mockResponse));
    }

    @GetMapping("/system/ai-performance")
    @Operation(summary = "AI Performance", description = "Metrics for AI model latency and accuracy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiPerformance() {
        log.info("Admin requesting AI performance metrics");
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("averageLatencyMs", 350);
        mockResponse.put("phonemeAccuracyRate", 0.88);
        return ResponseEntity.ok(ApiResponse.success("Thành công", mockResponse));
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
}
