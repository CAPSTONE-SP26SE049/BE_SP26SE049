package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.*;
import org.fsa_2026.company_fsa_captone_2026.entity.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Service
 * Handles admin-specific business logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AccountRepository accountRepository;
    private final LearningUnitRepository learningUnitRepository;
    private final ContentItemRepository contentItemRepository;
    private final StudySessionRepository studySessionRepository;
    private final SessionDetailRepository sessionDetailRepository;
    private final ContentApprovalHistoryRepository contentApprovalHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
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
        return contentItemRepository.findByType("PRONUNCIATION").stream()
                .map(ChallengeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Create a new Challenge
     */
    @Transactional
    public ChallengeResponse createChallenge(ChallengeCreateRequest request) {
        LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        ContentItem challenge = new ContentItem();
        challenge.setLearningUnit(level);
        challenge.setType("PRONUNCIATION");
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
            challenge.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.error("Failed to serialize Challenge metadata", e);
        }

        challenge = contentItemRepository.save(challenge);
        return ChallengeResponse.fromEntity(challenge);
    }

    /**
     * Update an existing Challenge
     */
    @Transactional
    public ChallengeResponse updateChallenge(UUID id, ChallengeCreateRequest request) {
        ContentItem challenge = contentItemRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Challenge"));

        LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        challenge.setLearningUnit(level);
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
            challenge.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
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

    @Transactional(readOnly = true)
    public ChallengeResponse getChallengeById(UUID id) {
        ContentItem challenge = contentItemRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Challenge"));
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
        } catch (Exception e) {
            log.error("Failed to serialize Dialect metadata", e);
        }

        return DialectResponse.fromEntity(learningUnitRepository.save(dialect));
    }

    @Transactional
    public DialectResponse updateDialect(UUID id, DialectCreateRequest request) {
        LearningUnit dialect = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect"));

        dialect.setName(request.getName());
        
        try {
            Map<String, Object> metadata = new java.util.HashMap<>();
            if (dialect.getMetadataJson() != null) {
                metadata = objectMapper.readValue(dialect.getMetadataJson(), Map.class);
            }
            metadata.put("description", request.getDescription());
            dialect.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.error("Failed to update Dialect metadata", e);
        }

        return DialectResponse.fromEntity(learningUnitRepository.save(dialect));
    }

    @Transactional
    public void deleteDialect(UUID id) {
        if (!learningUnitRepository.existsById(id)) {
            throw new ApiException("NOT_FOUND", "Không tìm thấy Dialect");
        }
        learningUnitRepository.deleteById(id);
    }

    // ==========================================
    // 1c. Content Management: Levels
    // ==========================================

    @Transactional(readOnly = true)
    public List<LevelResponse> getAllLevels() {
        return learningUnitRepository.findAll().stream()
                .filter(unit -> "LEVEL".equals(unit.getType()))
                .map(LevelResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LevelResponse getLevelById(UUID id) {
        LearningUnit level = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));
        return LevelResponse.fromEntity(level);
    }

    @Transactional
    public LevelResponse createLevel(LevelCreateRequest request) {
        LearningUnit dialect = learningUnitRepository.findById(request.getDialectId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect"));

        LearningUnit level = LearningUnit.builder()
                .parent(dialect)
                .name(request.getName())
                .type("LEVEL")
                .build();

        try {
            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("level_order", request.getLevelOrder());
            metadata.put("description", request.getDescription());
            metadata.put("min_stars_required", request.getMinStarsRequired() != null ? request.getMinStarsRequired() : 0);
            if (request.getErrorTagId() != null) {
                metadata.put("error_tag_id", request.getErrorTagId().toString());
            }
            level.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.error("Failed to serialize Level metadata", e);
        }

        return LevelResponse.fromEntity(learningUnitRepository.save(level));
    }

    @Transactional
    public LevelResponse updateLevel(UUID id, LevelCreateRequest request) {
        LearningUnit level = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        if (level.getParent() == null || !level.getParent().getId().equals(request.getDialectId())) {
            LearningUnit dialect = learningUnitRepository.findById(request.getDialectId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect"));
            level.setParent(dialect);
        }

        level.setName(request.getName());

        try {
            Map<String, Object> metadata = new java.util.HashMap<>();
            if (level.getMetadataJson() != null) {
                metadata = objectMapper.readValue(level.getMetadataJson(), Map.class);
            }
            metadata.put("level_order", request.getLevelOrder());
            metadata.put("description", request.getDescription());
            if (request.getMinStarsRequired() != null) {
                metadata.put("min_stars_required", request.getMinStarsRequired());
            }
            if (request.getErrorTagId() != null) {
                metadata.put("error_tag_id", request.getErrorTagId().toString());
            }
            level.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.error("Failed to update Level metadata", e);
        }

        return LevelResponse.fromEntity(learningUnitRepository.save(level));
    }

    @Transactional(readOnly = true)
    public List<LevelResponse> getPendingLevels() {
        return learningUnitRepository.findByType("LEVEL").stream()
                .filter(unit -> {
                    try {
                        if (unit.getMetadataJson() != null) {
                            Map<String, Object> metadata = objectMapper.readValue(unit.getMetadataJson(), Map.class);
                            return "PENDING".equals(metadata.get("status"));
                        }
                    } catch (Exception e) { }
                    return false;
                })
                .map(LevelResponse::fromEntity)
                .collect(Collectors.toList());
    }

        @Transactional(readOnly = true)
    public List<ChallengeResponse> getPendingChallenges() {
        return contentItemRepository.findAll().stream()
                .filter(item -> "PRONUNCIATION".equals(item.getType()) && "PENDING".equals(item.getStatus()))
                .map(ChallengeResponse::fromEntity)
                .collect(Collectors.toList());
    }

        @Transactional
    public LevelResponse reviewLevel(UUID id, org.fsa_2026.company_fsa_captone_2026.dto.ContentReviewRequest request) {
        LearningUnit level = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        try {
            java.util.Map<String, Object> metadata = new java.util.HashMap<>();
            if (level.getMetadataJson() != null) {
                metadata = objectMapper.readValue(level.getMetadataJson(), java.util.Map.class);
            }
            metadata.put("status", request.getStatus().name());
            if (org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.REJECTED.equals(request.getStatus())) {
                metadata.put("rejection_reason", request.getRejectionReason());
            } else {
                metadata.remove("rejection_reason");
            }
            level.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.error("Failed to update Level status", e);
        }

        level = learningUnitRepository.save(level);

        // Record history
        String contentSnapshot = "";
        try {
            contentSnapshot = objectMapper.writeValueAsString(LevelResponse.fromEntity(level));
        } catch (Exception e) { }

        ContentApprovalHistory history = ContentApprovalHistory.builder()
                .contentType("LEVEL")
                .contentId(level.getId())
                .status(request.getStatus())
                .comment(request.getComment() != null && !request.getComment().isBlank() ? request.getComment()
                        : request.getRejectionReason())
                .contentSnapshot(contentSnapshot)
                .build();
        contentApprovalHistoryRepository.save(history);

        return LevelResponse.fromEntity(level);
    }

        @Transactional
    public ChallengeResponse reviewChallenge(UUID id, org.fsa_2026.company_fsa_captone_2026.dto.ContentReviewRequest request) {
        ContentItem challenge = contentItemRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Challenge"));

        challenge.setStatus(request.getStatus().name());

        if (org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.REJECTED.name().equals(request.getStatus().name())) {
            try {
                java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                if (challenge.getMetadataJson() != null) {
                    metadata = objectMapper.readValue(challenge.getMetadataJson(), java.util.Map.class);
                }
                metadata.put("rejection_reason", request.getRejectionReason());
                challenge.setMetadataJson(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) { }
        }

        challenge = contentItemRepository.save(challenge);

        // Record history
        String contentSnapshot = "";
        try {
            contentSnapshot = objectMapper.writeValueAsString(ChallengeResponse.fromEntity(challenge));
        } catch (Exception e) { }

        ContentApprovalHistory history = ContentApprovalHistory.builder()
                .contentType("CHALLENGE")
                .contentId(challenge.getId())
                .status(request.getStatus())
                .comment(request.getComment() != null && !request.getComment().isBlank() ? request.getComment()
                        : request.getRejectionReason())
                .contentSnapshot(contentSnapshot)
                .build();
        contentApprovalHistoryRepository.save(history);

        return ChallengeResponse.fromEntity(challenge);
    }

        @Transactional(readOnly = true)
    public List<QuizResponse> getPendingQuizzes() {
        return contentItemRepository.findAll().stream()
                .filter(item -> "QUIZ".equals(item.getType()) && "PENDING".equals(item.getStatus()))
                .map(QuizResponse::fromEntity)
                .collect(Collectors.toList());
    }

        @Transactional(readOnly = true)
    public QuizResponse getQuizById(UUID id) {
        ContentItem quiz = contentItemRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Quiz"));
        return QuizResponse.fromEntity(quiz);
    }

        @Transactional
    public QuizResponse reviewQuiz(UUID id, org.fsa_2026.company_fsa_captone_2026.dto.ContentReviewRequest request) {
        ContentItem quiz = contentItemRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Quiz"));

        quiz.setStatus(request.getStatus().name());

        if (org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.REJECTED.name().equals(request.getStatus().name())) {
            try {
                java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                if (quiz.getMetadataJson() != null) {
                    metadata = objectMapper.readValue(quiz.getMetadataJson(), java.util.Map.class);
                }
                metadata.put("rejection_reason", request.getRejectionReason());
                quiz.setMetadataJson(objectMapper.writeValueAsString(metadata));
            } catch (Exception e) { }
        }

        quiz = contentItemRepository.save(quiz);

        // Record history
        String contentSnapshot = "";
        try {
            contentSnapshot = objectMapper.writeValueAsString(QuizResponse.fromEntity(quiz));
        } catch (Exception e) { }

        ContentApprovalHistory history = ContentApprovalHistory.builder()
                .contentType("QUIZ")
                .contentId(quiz.getId())
                .status(request.getStatus())
                .comment(request.getComment() != null && !request.getComment().isBlank() ? request.getComment()
                        : request.getRejectionReason())
                .contentSnapshot(contentSnapshot)
                .build();
        contentApprovalHistoryRepository.save(history);

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
            throw new ApiException("NOT_FOUND", "Không tìm thấy Level");
        }
        learningUnitRepository.deleteById(id);
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
            long totalUsers = accountRepository.count();
            // TODO: Update analytics with proper JSONB aggregation for scores if needed
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
}
