package org.fsa_2026.company_fsa_captone_2026.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fsa_2026.company_fsa_captone_2026.dto.AnalyticsOverviewResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserAnalyticsResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ContentApprovalHistoryResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.RegisterResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserManagementResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.UserStatusUpdateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.UserUpdateRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.dto.RewardCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.RewardResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.ContentItem;
import org.fsa_2026.company_fsa_captone_2026.entity.DailyAnalytics;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ContentApprovalHistoryRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ContentItemRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.DailyAnalyticsRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.RewardCatalogRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.SpeakingAttemptRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.StudySessionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Service
 * Handles admin-specific business logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();
    private static final String CODE_NOT_FOUND = "NOT_FOUND";
    private static final String TYPE_PRONUNCIATION = "PRONUNCIATION";
    private static final String TYPE_LEVEL = "LEVEL";
    private static final String MSG_USER_NOT_FOUND = "Không tìm thấy người dùng";
    private static final String MSG_LEVEL_NOT_FOUND = "Không tìm thấy Level";
    private static final String MSG_CHALLENGE_NOT_FOUND = "Không tìm thấy Challenge";

    private final AccountRepository accountRepository;
    private final LearningUnitRepository learningUnitRepository;
    private final AccountLearningUnitRepository accountLearningUnitRepository;
    private final ContentItemRepository contentItemRepository;
    private final StudySessionRepository studySessionRepository;
    private final SpeakingAttemptRepository speakingAttemptRepository;
    private final ContentApprovalHistoryRepository contentApprovalHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final DailyAnalyticsRepository dailyAnalyticsRepository;
    private final ObjectMapper objectMapper;
    private final RewardCatalogRepository rewardCatalogRepository;

    /**
     * Create a new Educator account
     *
     * @param request thông tin educator cần tạo
     * @return thông tin tài khoản educator sau khi tạo
     */
    @Transactional
    public RegisterResponse createEducatorAccount(EducatorCreateRequest request) {
        // Validate uniqueness
        Map<String, String> errors = new HashMap<>();

        if (accountRepository.existsByEmail(request.getEmail())) {
            errors.put("email", "Email đã tồn tại");
        }

        if (!errors.isEmpty()) {
            throw new AuthService.ValidationException("Xác thực dữ liệu thất bại", errors);
        }

        // Generate educator password (emailPrefix + random number + @)
        String prefix = request.getEmail().split("@")[0];
        int randomNum = 1000 + SECURE_RANDOM.nextInt(9000);
        String generatedPassword = prefix + randomNum + "@";

        // Create Account with role EDUCATOR
        Account account = Account.createUserAccount(
                request.getEmail(),
                passwordEncoder.encode(generatedPassword),
                null, // Phone is null for educator creation
                null); // Region is null

        // Set properties specifically for Educator
        account.setRoleCode(RoleCode.EDUCATOR);
        account.setEmailVerified(true); // Always verified since admin created it
        account.setFullName(request.getFullName());
        account.setAvatarUrl(generateDefaultAvatar(request.getFullName()));

        account = accountRepository.save(account);

        log.info("Admin created Educator account successfully: email={}", request.getEmail());

        // Build response
        RegisterResponse response = RegisterResponse.builder()
                .id(account.getId().toString())
                .email(account.getEmail())
                .fullName(request.getFullName())
                .role(account.getRoleCode().name())
                .build();

        // Send email with generated password to Educator
        try {
            emailService.sendEducatorAccountCreatedEmail(request.getEmail(), request.getFullName(), generatedPassword);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email mật khẩu đến Educator: {} - {}", request.getEmail(), e.getMessage(), e);
        }

        return response;
    }

    /**
     * Create a new user account with a specified role (USER or EDUCATOR)
     *
     * @param email    email của user mới
     * @param fullName họ tên
     * @param roleCode vai trò: USER hoặc EDUCATOR
     * @return thông tin tài khoản sau khi tạo
     */
    @Transactional
    public RegisterResponse createUserWithRole(String email, String fullName, String roleCode) {
        if (accountRepository.existsByEmail(email)) {
            throw new ApiException("CONFLICT", "Email đã tồn tại trong hệ thống");
        }

        RoleCode role;
        try {
            role = RoleCode.valueOf(roleCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("BAD_REQUEST", "Vai trò không hợp lệ: " + roleCode);
        }

        // Do not allow creating ADMIN via this endpoint
        if (role == RoleCode.ADMIN) {
            throw new ApiException("FORBIDDEN", "Không thể tạo tài khoản Admin qua chức năng này");
        }

        String prefix = email.split("@")[0];
        int randomNum = 1000 + SECURE_RANDOM.nextInt(9000);
        String generatedPassword = prefix + randomNum + "@";

        Account account = Account.createUserAccount(email, passwordEncoder.encode(generatedPassword), null, null);
        account.setRoleCode(role);
        account.setEmailVerified(true);
        account.setFullName(fullName);
        account.setAvatarUrl(generateDefaultAvatar(fullName));
        account = accountRepository.save(account);

        log.info("Admin created {} account: email={}", role, email);

        RegisterResponse response = RegisterResponse.builder()
                .id(account.getId().toString())
                .email(account.getEmail())
                .fullName(fullName)
                .role(account.getRoleCode().name())
                .build();

        try {
            emailService.sendEducatorAccountCreatedEmail(email, fullName, generatedPassword);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email mật khẩu: {} - {}", email, e.getMessage(), e);
        }

        return response;
    }

    // ==========================================
    // User Management
    // ==========================================

    @Transactional(readOnly = true)
    public List<UserAnalyticsResponse> getUsersAnalytics() {
        // 1. Fetch only users (exclude Educator and Admin)
        List<Account> users = accountRepository.findAllByRoleCodeIn(List.of(RoleCode.USER));

        // 2. Count total quizzes in system
        long totalQuizzes = learningUnitRepository.countByType("QUIZ");

        // 3. Bulk fetch progress for all users to avoid N+1 issue
        List<UUID> userIds = users.stream().map(Account::getId).collect(Collectors.toList());
        List<AccountLearningUnitRepository.UserProgressProjection> progressList = accountLearningUnitRepository
                .findProgressByAccountIds(userIds);

        // Map for quick lookup O(1)
        Map<UUID, AccountLearningUnitRepository.UserProgressProjection> progressMap = progressList.stream()
                .collect(Collectors.toMap(
                        AccountLearningUnitRepository.UserProgressProjection::getAccountId,
                        p -> p));

        return users.stream().map(user -> {
            AccountLearningUnitRepository.UserProgressProjection p = progressMap.get(user.getId());
            return UserAnalyticsResponse.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .completedQuizzes(p != null ? p.getCompletedCount().intValue() : 0)
                    .totalQuizzes((int) totalQuizzes)
                    .totalStars(user.getTotalStars() != null ? user.getTotalStars() : 0)
                    .currentStreak(user.getCurrentStreakDays() != null ? user.getCurrentStreakDays() : 0)
                    .averageScore(p != null ? p.getAverageScore() : 0.0)
                    .build();
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getAiPerformance() {
        Double avgLatency = speakingAttemptRepository.getAverageProcessingTimeMs();
        Double accuracyRate = speakingAttemptRepository.getAccuracyRate();

        Map<String, Object> response = new HashMap<>();
        response.put("averageLatencyMs", avgLatency != null ? Math.round(avgLatency) : 0);
        response.put("accuracyRate", accuracyRate != null ? accuracyRate : 0.0);
        return response;
    }

    @Transactional(readOnly = true)
    public List<UserManagementResponse> getAllUsers() {
        // Lấy toàn bộ user/educator, bao gồm cả active và inactive
        return accountRepository
                .findAllByRoleCodeIn(List.of(RoleCode.USER, RoleCode.EDUCATOR))
                .stream()
                .map(UserManagementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserManagementResponse getUserById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_USER_NOT_FOUND));
        return UserManagementResponse.fromEntity(account);
    }

    @Transactional
    public UserManagementResponse updateUserStatus(UUID id, UserStatusUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_USER_NOT_FOUND));

        account.setIsActive(request.getIsActive());
        account = accountRepository.save(account);

        return UserManagementResponse.fromEntity(account);
    }

    @Transactional
    public UserManagementResponse updateUser(UUID id, UserUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_USER_NOT_FOUND));

        if (request.getPhone() != null) {
            account.setPhone(request.getPhone());
        }
        if (request.getFullName() != null) {
            account.setFullName(request.getFullName());
        }

        account = accountRepository.save(account);

        return UserManagementResponse.fromEntity(account);
    }

    // ==========================================
    // 1a. Content Management: Challenges
    // ==========================================
    @Transactional(readOnly = true)
    public List<ChallengeResponse> getAllChallenges() {
        return contentItemRepository.findByType(TYPE_PRONUNCIATION).stream()
                .map(ChallengeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Create a new Challenge
     *
     * @param request dữ liệu challenge cần tạo
     * @return challenge đã được lưu
     */
    @Transactional
    public ChallengeResponse createChallenge(ChallengeCreateRequest request) {
        LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));

        ContentItem challenge = new ContentItem();
        challenge.setLearningUnit(level);
        challenge.setType(TYPE_PRONUNCIATION);
        challenge.setStatus("APPROVED");
        challenge.setTitle(request.getContentText());

        try {
            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("skill_type", request.getSkillType());
            metadata.put("content_text", request.getContentText());
            metadata.put("phonetic_transcription_ipa", request.getPhoneticTranscriptionIpa());
            metadata.put("reference_audio_url", request.getReferenceAudioUrl());
            metadata.put("focus_phonemes", request.getFocusPhonemes());
            metadata.put("status", "APPROVED");
            challenge.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Challenge metadata", e);
        }

        challenge = contentItemRepository.save(challenge);
        return ChallengeResponse.fromEntity(challenge);
    }

    /**
     * Update an existing Challenge
     *
     * @param id      id challenge cần cập nhật
     * @param request dữ liệu challenge mới
     * @return challenge sau khi cập nhật
     */
    @Transactional
    public ChallengeResponse updateChallenge(UUID id, ChallengeCreateRequest request) {
        ContentItem challenge = contentItemRepository.findById(id)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_CHALLENGE_NOT_FOUND));

        LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));

        challenge.setLearningUnit(level);
        challenge.setTitle(request.getContentText());
        challenge.setStatus("APPROVED");

        try {
            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("skill_type", request.getSkillType());
            metadata.put("content_text", request.getContentText());
            metadata.put("phonetic_transcription_ipa", request.getPhoneticTranscriptionIpa());
            metadata.put("reference_audio_url", request.getReferenceAudioUrl());
            metadata.put("focus_phonemes", request.getFocusPhonemes());
            metadata.put("status", "APPROVED");
            challenge.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            log.error("Failed to update Challenge metadata", e);
        }

        challenge = contentItemRepository.save(challenge);
        return ChallengeResponse.fromEntity(challenge);
    }

    /**
     * Delete a Challenge
     */

    // removed - use EducatorService.deleteChallenge

    // ==========================================
    // 1b. Content Management: Dialects
    // ==========================================

    /**
     * Lấy chi tiết challenge theo id.
     *
     * @param id id challenge
     * @return challenge response tương ứng
     */
    @Transactional(readOnly = true)
    public ChallengeResponse getChallengeById(UUID id) {
        ContentItem challenge = contentItemRepository.findById(id)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_CHALLENGE_NOT_FOUND));
        return ChallengeResponse.fromEntity(challenge);
    }


    // ==========================================
    // 1c. Content Management: Levels
    // ==========================================

    @Transactional(readOnly = true)
    public List<LevelResponse> getAllLevels() {
        return learningUnitRepository.findAll().stream()
                .filter(unit -> TYPE_LEVEL.equals(unit.getType()))
                .filter(this::isNotDeleted)
                .map(LevelResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LevelResponse getLevelById(UUID id) {
        LearningUnit level = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));
        return LevelResponse.fromEntity(level);
    }

    @Transactional
    public LevelResponse createLevel(LevelCreateRequest request) {
        LearningUnit parent = null;
        if (request.getParentId() != null) {
            parent = learningUnitRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy parent"));
        }

        LearningUnit level = LearningUnit.builder()
                .parent(parent)
                .name(request.getName())
                .type(TYPE_LEVEL)
                .build();

        try {
            Map<String, Object> metadata = request.getMetadataJson() != null
                    ? new java.util.HashMap<>(request.getMetadataJson())
                    : new java.util.HashMap<>();
            metadata.put("status", "APPROVED");
            level.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Level metadata", e);
        }

        return LevelResponse.fromEntity(learningUnitRepository.save(level));
    }

    @Transactional
    public LevelResponse updateLevel(UUID id, LevelCreateRequest request) {
        LearningUnit level = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));

        if (request.getParentId() != null
                && (level.getParent() == null || !level.getParent().getId().equals(request.getParentId()))) {
            LearningUnit parent = learningUnitRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy parent"));
            level.setParent(parent);
        }

        level.setName(request.getName());
        level.setType(request.getType());

        try {
            Map<String, Object> metadata = request.getMetadataJson() != null
                    ? new java.util.HashMap<>(request.getMetadataJson())
                    : new java.util.HashMap<>();
            metadata.put("status", "APPROVED");
            level.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            log.error("Failed to update Level metadata", e);
        }

        return LevelResponse.fromEntity(learningUnitRepository.save(level));
    }

    @Transactional(readOnly = true)
    public QuizResponse getQuizById(UUID id) {
        ContentItem quiz = contentItemRepository.findById(id)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy Quiz"));
        return QuizResponse.fromEntity(quiz);
    }

    @Transactional(readOnly = true)
    public List<ContentApprovalHistoryResponse> getContentApprovalHistory(UUID contentId) {
        return contentApprovalHistoryRepository.findByContentIdOrderByCreatedAtDesc(contentId).stream()
                .map(ContentApprovalHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteLevel(UUID id) {
        LearningUnit level = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));

        List<LearningUnit> children = learningUnitRepository.findByParentId(id);
        boolean hasActiveChild = children.stream().anyMatch(this::isNotDeleted);
        if (hasActiveChild) {
            throw new ApiException("CONFLICT", "Không thể xóa màn học do vẫn còn bài kiểm tra / bài học bên trong. Vui lòng xóa các mục con trước.");
        }

        // Xóa toàn bộ progress của user liên quan đến màn học này
        accountLearningUnitRepository.deleteByLearningUnitId(id);

        // Hard delete khỏi database
        learningUnitRepository.deleteById(id);
        log.info("Level {} đã được xóa cứng khỏi database", id);
    }

    private boolean isNotDeleted(LearningUnit unit) {
        if (unit.getMetadataJson() == null || unit.getMetadataJson().isBlank()) return true;
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                    unit.getMetadataJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            return !"DELETED".equals(metadata.get("status"));
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get Analytics Overview from DB using DailyAnalytics table to cache data
     *
     * @return tổng quan analytics toàn hệ thống
     */
    @Transactional
    public AnalyticsOverviewResponse getAnalyticsOverview() {
        LocalDate today = LocalDate.now();
        Optional<DailyAnalytics> analyticsOpt = dailyAnalyticsRepository.findByRecordDate(today);

        long totalUsers = accountRepository.count();
        long totalAttempts = speakingAttemptRepository.countByConsentGivenTrue();
        double averageScore = speakingAttemptRepository.averageGroqScoreWithConsentGivenTrue();
        java.time.Instant sevenDaysAgo = java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
        long activeUsers7Days = studySessionRepository.countDistinctAccountByStartedAtAfter(sevenDaysAgo);

        DailyAnalytics analytics;
        if (analyticsOpt.isPresent()) {
            analytics = analyticsOpt.get();
            analytics.setTotalUsers(totalUsers);
            analytics.setTotalAttempts(totalAttempts);
            analytics.setAverageScore(averageScore);
            analytics.setActiveUsers(activeUsers7Days);
            analytics = dailyAnalyticsRepository.save(analytics);
        } else {
            analytics = new DailyAnalytics();
            analytics.setRecordDate(today);
            analytics.setTotalUsers(totalUsers);
            analytics.setTotalAttempts(totalAttempts);
            analytics.setAverageScore(averageScore);
            analytics.setActiveUsers(activeUsers7Days);
            analytics = dailyAnalyticsRepository.save(analytics);
        }

        return AnalyticsOverviewResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers7Days(activeUsers7Days)
                .totalAttempts(totalAttempts)
                .averageScore(averageScore)
                .build();
    }

    /**
     * Get Error Heatmaps from DB by dialect
     * Rate = (count groq_score < 80) / (total count)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getErrorHeatmaps() {
        Map<String, Object> stats = new HashMap<>();
        String[] dialects = { "NORTH", "SOUTH", "CENTRAL" };

        for (String d : dialects) {
            long total = speakingAttemptRepository.countByDialectAndConsentGivenTrue(d);
            long errors = speakingAttemptRepository.countByDialectAndConsentGivenTrueAndGroqScoreLessThan(d, 80);

            double rate = total > 0 ? (double) errors / total : 0.0;
            stats.put(d.toLowerCase(), rate);
        }
        return stats;
    }

    /**
     * Generate DiceBear avatar URL from name
     */
    private String generateDefaultAvatar(String fullName) {
        String seed = fullName != null ? fullName.replaceAll("\\s+", "+") : "default";
        return "https://api.dicebear.com/7.x/initials/svg?seed=" + seed;
    }

    // ==========================================
    // Reward Catalog Management (Badge/Achievement)
    // ==========================================

    @Transactional(readOnly = true)
    public List<RewardResponse> getAllRewards() {
        return rewardCatalogRepository.findAll()
                .stream()
                .map(this::enrichRewardResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RewardResponse getRewardById(UUID id) {
        RewardCatalog reward = rewardCatalogRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy phần thưởng"));
        return enrichRewardResponse(reward);
    }

    @Transactional
    public RewardResponse createReward(RewardCreateRequest request) {
        RewardCatalog reward = RewardCatalog.builder()
                .code(request.getCode())
                .name(request.getName())
                .rewardType(org.fsa_2026.company_fsa_captone_2026.entity.enums.RewardType.BADGE)
                .iconUrl(request.getIconUrl())
                .xpReward(0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        return enrichRewardResponse(rewardCatalogRepository.save(reward));
    }

    @Transactional
    public RewardResponse updateReward(UUID id, RewardCreateRequest request) {
        RewardCatalog reward = rewardCatalogRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy phần thưởng"));
        reward.setCode(request.getCode());
        reward.setName(request.getName());
        reward.setIconUrl(request.getIconUrl());
        reward.setActive(request.getIsActive() != null ? request.getIsActive() : reward.isActive());
        return enrichRewardResponse(rewardCatalogRepository.save(reward));
    }

    @Transactional
    public RewardResponse toggleRewardActive(UUID id) {
        RewardCatalog reward = rewardCatalogRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy phần thưởng"));
        reward.setActive(!reward.isActive());
        return enrichRewardResponse(rewardCatalogRepository.save(reward));
    }

    @Transactional
    public void deleteReward(UUID id) {
        if (!rewardCatalogRepository.existsById(id)) {
            throw new ApiException("NOT_FOUND", "Không tìm thấy phần thưởng");
        }
        
        // Check if any quiz is linked to this reward
        Optional<LearningUnit> linkedQuiz = learningUnitRepository.findByRewardCatalogId(id);
        if (linkedQuiz.isPresent()) {
            throw new ApiException("CONFLICT", "Không thể xóa phần thưởng này vì đang được gán cho bài kiểm tra: " + linkedQuiz.get().getName());
        }

        rewardCatalogRepository.deleteById(id);
    }

    /**
     * Attach/Detach a reward to a quiz.
     */
    @Transactional
    public void attachRewardToQuiz(UUID rewardId, UUID quizId) {
        LearningUnit quiz = learningUnitRepository.findById(quizId)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy bài kiểm tra"));

        RewardCatalog reward = rewardCatalogRepository.findById(rewardId)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy thành tựu"));

        // Rule: Each reward can only be assigned to one quiz
        Optional<LearningUnit> otherQuiz = learningUnitRepository.findByRewardCatalogId(rewardId);
        if (otherQuiz.isPresent() && !otherQuiz.get().getId().equals(quizId)) {
            throw new ApiException("CONFLICT",
                    "Thành tựu này đã được gán cho bài kiểm tra: " + otherQuiz.get().getName());
        }

        // Toggle logic
        if (quiz.getRewardCatalog() != null && quiz.getRewardCatalog().getId().equals(rewardId)) {
            quiz.setRewardCatalog(null);
            log.info("Detached reward {} from quiz {}", rewardId, quizId);
        } else {
            quiz.setRewardCatalog(reward);
            log.info("Attached reward {} to quiz {}", rewardId, quizId);
        }

        learningUnitRepository.save(quiz);
    }

    /**
     * Enrich RewardResponse with linked quiz/level information.
     */
    private RewardResponse enrichRewardResponse(RewardCatalog reward) {
        RewardResponse response = RewardResponse.fromEntity(reward);

        // Find the quiz that this reward is linked to
        Optional<LearningUnit> linkedQuiz = learningUnitRepository.findByRewardCatalogId(reward.getId());
        if (linkedQuiz.isPresent()) {
            LearningUnit quiz = linkedQuiz.get();
            response.setLinkedQuizId(quiz.getId());
            response.setLinkedQuizName(quiz.getName());

            // Get the parent level name
            if (quiz.getParent() != null) {
                response.setLinkedLevelName(quiz.getParent().getName());
            }
        }

        return response;
    }
}
