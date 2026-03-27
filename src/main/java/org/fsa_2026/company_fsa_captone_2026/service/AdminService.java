package org.fsa_2026.company_fsa_captone_2026.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fsa_2026.company_fsa_captone_2026.dto.AnalyticsOverviewResponse;
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
import org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ContentApprovalHistoryRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ContentItemRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.DailyAnalyticsRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.RewardCatalogRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
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
    private final ContentItemRepository contentItemRepository;
    private final StudySessionRepository studySessionRepository;
    private final ContentApprovalHistoryRepository contentApprovalHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final DailyAnalyticsRepository dailyAnalyticsRepository;
    private final ObjectMapper objectMapper;
    private final RewardCatalogRepository rewardCatalogRepository;
    private final AccountLearningUnitRepository accountLearningUnitRepository;

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

    // ==========================================
    // User Management
    // ==========================================

    @Transactional(readOnly = true)
    public List<UserManagementResponse> getAllUsers() {
        // Filter tại DB thay vì load toàn bộ rồi filter bằng Java
        return accountRepository
                .findAllActiveByRoleCodeIn(List.of(RoleCode.USER, RoleCode.EDUCATOR))
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
            if (request.getDifficulty() != null) {
                metadata.put("difficulty", request.getDifficulty().name());
            }
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
            if (request.getDifficulty() != null) {
                metadata.put("difficulty", request.getDifficulty().name());
            }
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

    @Transactional
    public DialectResponse createDialect(DialectCreateRequest request) {
        LearningUnit dialect = LearningUnit.builder()
                .name(request.getName())
                .type("DIALECT")
                .build();

        try {
            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("description", request.getDescription());
            dialect.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Dialect metadata", e);
        }

        return DialectResponse.fromEntity(learningUnitRepository.save(dialect));
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public DialectResponse updateDialect(UUID id, DialectCreateRequest request) {
        LearningUnit dialect = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy Dialect"));

        dialect.setName(request.getName());

        try {
            Map<String, Object> metadata = new java.util.HashMap<>();
            if (dialect.getMetadataJson() != null) {
                metadata = objectMapper.readValue(dialect.getMetadataJson(), Map.class);
            }
            metadata.put("description", request.getDescription());
            dialect.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            log.error("Failed to update Dialect metadata", e);
        }

        return DialectResponse.fromEntity(learningUnitRepository.save(dialect));
    }

    @Transactional
    public void deleteDialect(UUID id) {
        if (!learningUnitRepository.existsById(id)) {
            throw new ApiException(CODE_NOT_FOUND, "Không tìm thấy Dialect");
        }
        deleteUnit(id);
    }

    // ==========================================
    // 1c. Content Management: Levels
    // ==========================================

    @Transactional(readOnly = true)
    public List<LevelResponse> getAllLevels() {
        return learningUnitRepository.findAll().stream()
                .filter(unit -> TYPE_LEVEL.equals(unit.getType()))
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
        if (!learningUnitRepository.existsById(id)) {
            throw new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND);
        }
        deleteUnit(id);
    }

    /**
     * Recursive deletion of a LearningUnit and all its dependencies
     */
    @Transactional
    private void deleteUnit(UUID id) {
        // 1. Find all children
        List<LearningUnit> children = learningUnitRepository.findByParentId(id);
        for (LearningUnit child : children) {
            deleteUnit(child.getId());
        }

        // 2. Delete all ContentItems (Quizzes/Challenges) referencing this unit
        contentItemRepository.deleteByLearningUnitId(id);

        // 3. Delete all AccountLearningUnit entries (User progress)
        accountLearningUnitRepository.deleteByLearningUnitId(id);

        // 4. Finally delete the unit itself
        learningUnitRepository.deleteById(id);
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

        DailyAnalytics analytics;
        if (analyticsOpt.isPresent()) {
            analytics = analyticsOpt.get(); // Trả về data đã gom trong ngày để tránh query lớn
        } else {
            long totalUsers = accountRepository.count();
            // Update analytics with proper JSONB aggregation for scores if needed
            long totalAttempts = studySessionRepository.count();

            double averageScore = 0.0; // Placeholder due to schema change

            java.time.Instant sevenDaysAgo = java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
            long activeUsers7Days = studySessionRepository.countDistinctAccountByStartedAtAfter(sevenDaysAgo);

            analytics = new DailyAnalytics();
            analytics.setRecordDate(today);
            analytics.setTotalUsers(totalUsers);
            analytics.setTotalAttempts(totalAttempts);
            analytics.setAverageScore(averageScore);
            analytics.setActiveUsers(activeUsers7Days);

            analytics = dailyAnalyticsRepository.save(analytics);
        }

        return AnalyticsOverviewResponse.builder()
                .totalUsers(analytics.getTotalUsers())
                .activeUsers7Days(analytics.getActiveUsers())
                .totalAttempts(analytics.getTotalAttempts())
                .averageScore(analytics.getAverageScore())
                .build();
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
                .stream().map(RewardResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RewardResponse getRewardById(UUID id) {
        RewardCatalog reward = rewardCatalogRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy phần thưởng"));
        return RewardResponse.fromEntity(reward);
    }

    @Transactional
    public RewardResponse createReward(RewardCreateRequest request) {
        RewardCatalog reward = RewardCatalog.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .rewardType(org.fsa_2026.company_fsa_captone_2026.entity.enums.RewardType.BADGE)
                .category(request.getCategory())
                .iconUrl(request.getIconUrl())
                .criteriaJson(request.getCriteriaJson())
                .xpReward(0)
                .isActive(request.isActive())
                .build();
        return RewardResponse.fromEntity(rewardCatalogRepository.save(reward));
    }

    @Transactional
    public RewardResponse updateReward(UUID id, RewardCreateRequest request) {
        RewardCatalog reward = rewardCatalogRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy phần thưởng"));
        reward.setCode(request.getCode());
        reward.setName(request.getName());
        reward.setDescription(request.getDescription());
        reward.setCategory(request.getCategory());
        reward.setIconUrl(request.getIconUrl());
        reward.setCriteriaJson(request.getCriteriaJson());
        reward.setActive(request.isActive());
        return RewardResponse.fromEntity(rewardCatalogRepository.save(reward));
    }

    @Transactional
    public RewardResponse toggleRewardActive(UUID id) {
        RewardCatalog reward = rewardCatalogRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy phần thưởng"));
        reward.setActive(!reward.isActive());
        return RewardResponse.fromEntity(rewardCatalogRepository.save(reward));
    }

    @Transactional
    public void deleteReward(UUID id) {
        if (!rewardCatalogRepository.existsById(id)) {
            throw new ApiException("NOT_FOUND", "Không tìm thấy phần thưởng");
        }
        rewardCatalogRepository.deleteById(id);
    }
}
