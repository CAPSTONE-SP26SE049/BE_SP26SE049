package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.RegisterResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.Challenge;
import org.fsa_2026.company_fsa_captone_2026.entity.Level;
import org.fsa_2026.company_fsa_captone_2026.entity.UserProfile;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AttemptRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LevelRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.UserProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.fsa_2026.company_fsa_captone_2026.entity.Dialect;
import org.fsa_2026.company_fsa_captone_2026.repository.DialectRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ErrorTagRepository;

import org.fsa_2026.company_fsa_captone_2026.repository.DailyAnalyticsRepository;
import org.fsa_2026.company_fsa_captone_2026.entity.DailyAnalytics;
import org.fsa_2026.company_fsa_captone_2026.entity.ContentApprovalHistory;
import org.fsa_2026.company_fsa_captone_2026.dto.ContentApprovalHistoryResponse;
import org.fsa_2026.company_fsa_captone_2026.repository.ContentApprovalHistoryRepository;
import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Admin Service
 * Handles admin-specific business logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChallengeRepository challengeRepository;
    private final LevelRepository levelRepository;
    private final DialectRepository dialectRepository;
    private final AttemptRepository attemptRepository;
    private final ContentApprovalHistoryRepository contentApprovalHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ErrorTagRepository errorTagRepository;
    private final DailyAnalyticsRepository dailyAnalyticsRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a new Educator account
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
        int randomNum = 1000 + new java.security.SecureRandom().nextInt(9000);
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

        account = accountRepository.save(account);

        // Create UserProfile
        UserProfile userProfile = UserProfile.builder()
                .account(account)
                .fullName(request.getFullName())
                .avatarUrl(generateDefaultAvatar(request.getFullName()))
                .build();

        userProfileRepository.save(userProfile);

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
        return accountRepository.findAll().stream()
                .filter(acc -> (acc.getRoleCode() == RoleCode.USER || acc.getRoleCode() == RoleCode.EDUCATOR)
                        && Boolean.TRUE.equals(acc.getIsActive()))
                .map(UserManagementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserManagementResponse getUserById(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));
        return UserManagementResponse.fromEntity(account);
    }

    @Transactional
    public UserManagementResponse updateUserStatus(UUID id, UserStatusUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        account.setIsActive(request.getIsActive());
        account = accountRepository.save(account);

        return UserManagementResponse.fromEntity(account);
    }

    @Transactional
    public UserManagementResponse updateUser(UUID id, UserUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        if (request.getPhone() != null) {
            account.setPhone(request.getPhone());
        }
        if (request.getRegion() != null) {
            account.setRegion(request.getRegion().toUpperCase());
        }

        account = accountRepository.save(account);

        UserProfile profile = account.getUserProfile();
        if (profile != null && request.getFullName() != null) {
            profile.setFullName(request.getFullName());
            userProfileRepository.save(profile);
        }

        return UserManagementResponse.fromEntity(account);
    }

    // ==========================================
    // 1a. Content Management: Challenges
    // ==========================================
    @Transactional(readOnly = true)
    public List<ChallengeResponse> getAllChallenges() {
        return challengeRepository.findAll().stream()
                .map(ChallengeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Create a new Challenge
     */
    @Transactional
    public ChallengeResponse createChallenge(ChallengeCreateRequest request) {
        Level level = levelRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        Challenge challenge = new Challenge();
        challenge.setLevel(level);
        challenge.setType(request.getType());
        challenge.setContentText(request.getContentText());
        challenge.setPhoneticTranscriptionIpa(request.getPhoneticTranscriptionIpa());
        challenge.setReferenceAudioUrl(request.getReferenceAudioUrl());
        challenge.setFocusPhonemes(request.getFocusPhonemes());

        challenge = challengeRepository.save(challenge);
        return ChallengeResponse.fromEntity(challenge);
    }

    /**
     * Update an existing Challenge
     */
    @Transactional
    public ChallengeResponse updateChallenge(UUID id, ChallengeCreateRequest request) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Challenge"));

        Level level = levelRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        challenge.setLevel(level);
        challenge.setType(request.getType());
        challenge.setContentText(request.getContentText());
        challenge.setPhoneticTranscriptionIpa(request.getPhoneticTranscriptionIpa());
        challenge.setReferenceAudioUrl(request.getReferenceAudioUrl());
        challenge.setFocusPhonemes(request.getFocusPhonemes());

        challenge = challengeRepository.save(challenge);
        return ChallengeResponse.fromEntity(challenge);
    }

    /**
     * Delete a Challenge
     */

    // removed - use EducatorService.deleteChallenge

    // ==========================================
    // 1b. Content Management: Dialects
    // ==========================================

    @Transactional(readOnly = true)
    public ChallengeResponse getChallengeById(UUID id) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Challenge"));
        return ChallengeResponse.fromEntity(challenge);
    }

    @Transactional
    public DialectResponse createDialect(DialectCreateRequest request) {
        Dialect dialect = Dialect.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return DialectResponse.fromEntity(dialectRepository.save(dialect));
    }

    @Transactional
    public DialectResponse updateDialect(UUID id, DialectCreateRequest request) {
        Dialect dialect = dialectRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect"));

        dialect.setName(request.getName());
        dialect.setDescription(request.getDescription());

        return DialectResponse.fromEntity(dialectRepository.save(dialect));
    }

    @Transactional
    public void deleteDialect(UUID id) {
        if (!dialectRepository.existsById(id)) {
            throw new ApiException("NOT_FOUND", "Không tìm thấy Dialect");
        }
        dialectRepository.deleteById(id);
    }

    // ==========================================
    // 1c. Content Management: Levels
    // ==========================================

    @Transactional(readOnly = true)
    public List<LevelResponse> getAllLevels() {
        return levelRepository.findAll().stream()
                .map(LevelResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LevelResponse getLevelById(UUID id) {
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));
        return LevelResponse.fromEntity(level);
    }

    @Transactional
    public LevelResponse createLevel(LevelCreateRequest request) {
        Dialect dialect = dialectRepository.findById(request.getDialectId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect"));

        org.fsa_2026.company_fsa_captone_2026.entity.ErrorTag errorTag = null;
        if (request.getErrorTagId() != null) {
            errorTag = errorTagRepository.findById(request.getErrorTagId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Error Tag"));
        }

        Level level = Level.builder()
                .dialect(dialect)
                .levelOrder(request.getLevelOrder())
                .name(request.getName())
                .description(request.getDescription())
                .minStarsRequired(request.getMinStarsRequired() != null ? request.getMinStarsRequired() : 0)
                .errorTag(errorTag)
                .build();

        return LevelResponse.fromEntity(levelRepository.save(level));
    }

    @Transactional
    public LevelResponse updateLevel(UUID id, LevelCreateRequest request) {
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        if (!level.getDialect().getId().equals(request.getDialectId())) {
            Dialect dialect = dialectRepository.findById(request.getDialectId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect"));
            level.setDialect(dialect);
        }

        level.setLevelOrder(request.getLevelOrder());
        level.setName(request.getName());
        level.setDescription(request.getDescription());
        if (request.getMinStarsRequired() != null) {
            level.setMinStarsRequired(request.getMinStarsRequired());
        }

        if (request.getErrorTagId() != null) {
            org.fsa_2026.company_fsa_captone_2026.entity.ErrorTag errorTag = errorTagRepository
                    .findById(request.getErrorTagId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Error Tag"));
            level.setErrorTag(errorTag);
        } else {
            level.setErrorTag(null);
        }

        return LevelResponse.fromEntity(levelRepository.save(level));
    }

    @Transactional(readOnly = true)
    public List<LevelResponse> getPendingLevels() {
        return levelRepository.findAll().stream()
                .filter(level -> org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.PENDING
                        .equals(level.getStatus()))
                .map(LevelResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChallengeResponse> getPendingChallenges() {
        return challengeRepository.findAll().stream()
                .filter(challenge -> org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.PENDING
                        .equals(challenge.getStatus()))
                .map(ChallengeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public LevelResponse reviewLevel(UUID id, org.fsa_2026.company_fsa_captone_2026.dto.ContentReviewRequest request) {
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        Level targetLevel = level;
        if (org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.APPROVED.equals(request.getStatus())) {
            if (level.getParent() != null) {
                targetLevel = level.getParent();
                targetLevel.setDialect(level.getDialect());
                targetLevel.setLevelOrder(level.getLevelOrder());
                targetLevel.setName(level.getName());
                targetLevel.setDescription(level.getDescription());
                targetLevel.setMinStarsRequired(level.getMinStarsRequired());
                targetLevel.setErrorTag(level.getErrorTag());
                targetLevel.setAiThreshold(level.getAiThreshold());
                targetLevel.setAudioUrl(level.getAudioUrl());
                targetLevel.setDraft(null);

                levelRepository.delete(level);
            } else {
                targetLevel.setStatus(request.getStatus());
                targetLevel.setRejectionReason(null);
            }
        } else if (org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.REJECTED
                .equals(request.getStatus())) {
            targetLevel.setStatus(request.getStatus());
            targetLevel.setRejectionReason(request.getRejectionReason());
        } else {
            targetLevel.setStatus(request.getStatus());
            targetLevel.setRejectionReason(null);
        }

        targetLevel = levelRepository.save(targetLevel);

        String contentSnapshot = "";
        try {
            contentSnapshot = objectMapper.writeValueAsString(LevelResponse.fromEntity(targetLevel));
        } catch (Exception e) {
            log.error("Failed to serialize Level content snapshot", e);
        }

        UUID historyContentId = (level.getParent() != null) ? level.getParent().getId() : level.getId();
        ContentApprovalHistory history = ContentApprovalHistory.builder()
                .contentType("LEVEL")
                .contentId(historyContentId)
                .status(request.getStatus())
                .comment(request.getComment() != null && !request.getComment().isBlank() ? request.getComment()
                        : request.getRejectionReason())
                .contentSnapshot(contentSnapshot)
                .build();
        contentApprovalHistoryRepository.save(history);

        return LevelResponse.fromEntity(targetLevel);
    }

    @Transactional
    public ChallengeResponse reviewChallenge(UUID id,
            org.fsa_2026.company_fsa_captone_2026.dto.ContentReviewRequest request) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Challenge"));

        Challenge targetChallenge = challenge;
        if (org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.APPROVED.equals(request.getStatus())) {
            if (challenge.getParent() != null) {
                targetChallenge = challenge.getParent();
                targetChallenge.setLevel(challenge.getLevel());
                targetChallenge.setType(challenge.getType());
                targetChallenge.setContentText(challenge.getContentText());
                targetChallenge.setPhoneticTranscriptionIpa(challenge.getPhoneticTranscriptionIpa());
                targetChallenge.setReferenceAudioUrl(challenge.getReferenceAudioUrl());
                targetChallenge.setFocusPhonemes(challenge.getFocusPhonemes());
                targetChallenge.setDraft(null);

                challengeRepository.delete(challenge);
            } else {
                targetChallenge.setStatus(request.getStatus());
                targetChallenge.setRejectionReason(null);
            }
        } else if (org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.REJECTED
                .equals(request.getStatus())) {
            targetChallenge.setStatus(request.getStatus());
            targetChallenge.setRejectionReason(request.getRejectionReason());
        } else {
            targetChallenge.setStatus(request.getStatus());
            targetChallenge.setRejectionReason(null);
        }

        targetChallenge = challengeRepository.save(targetChallenge);

        String contentSnapshot = "";
        try {
            contentSnapshot = objectMapper.writeValueAsString(ChallengeResponse.fromEntity(targetChallenge));
        } catch (Exception e) {
            log.error("Failed to serialize Challenge content snapshot", e);
        }

        UUID historyContentId = (challenge.getParent() != null) ? challenge.getParent().getId() : challenge.getId();
        ContentApprovalHistory history = ContentApprovalHistory.builder()
                .contentType("CHALLENGE")
                .contentId(historyContentId)
                .status(request.getStatus())
                .comment(request.getComment() != null && !request.getComment().isBlank() ? request.getComment()
                        : request.getRejectionReason())
                .contentSnapshot(contentSnapshot)
                .build();
        contentApprovalHistoryRepository.save(history);

        return ChallengeResponse.fromEntity(targetChallenge);
    }

    @Transactional(readOnly = true)
    public List<ContentApprovalHistoryResponse> getContentApprovalHistory(UUID contentId) {
        return contentApprovalHistoryRepository.findByContentIdOrderByCreatedAtDesc(contentId).stream()
                .map(ContentApprovalHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteLevel(UUID id) {
        if (!levelRepository.existsById(id)) {
            throw new ApiException("NOT_FOUND", "Không tìm thấy Level");
        }
        levelRepository.deleteById(id);
    }

    /**
     * Get Analytics Overview from DB using DailyAnalytics table to cache data
     */
    @Transactional
    public AnalyticsOverviewResponse getAnalyticsOverview() {
        LocalDate today = LocalDate.now();
        Optional<DailyAnalytics> analyticsOpt = dailyAnalyticsRepository.findByRecordDate(today);

        DailyAnalytics analytics;
        if (analyticsOpt.isPresent()) {
            analytics = analyticsOpt.get(); // Trả về data đã gom trong ngày để tránh query lớn
        } else {
            // Chỉ query nặng 1 lần đầu tiên trong ngày
            long totalUsers = accountRepository.count();
            long totalAttempts = attemptRepository.count();

            Double avgScore = attemptRepository.findAverageScore();
            double averageScore = avgScore != null ? Math.round(avgScore.doubleValue() * 10.0) / 10.0 : 0.0;

            java.time.Instant sevenDaysAgo = java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
            long activeUsers7Days = attemptRepository.countDistinctAccountByCreatedAtAfter(sevenDaysAgo);

            analytics = new DailyAnalytics();
            analytics.setRecordDate(today);
            analytics.setTotalUsers(totalUsers);
            analytics.setTotalAttempts(totalAttempts);
            analytics.setAverageScore(averageScore);
            analytics.setActiveUsers(activeUsers7Days);

            // Lưu lại vào bảng để gom dữ liệu
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
}
