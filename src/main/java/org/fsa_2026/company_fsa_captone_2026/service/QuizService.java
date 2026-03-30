package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.AccountBadgeResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelProgressResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCompleteRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCompleteResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.QuizQuestionRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.RewardResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountReward;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRewardRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.QuizChallengeItemRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.RewardCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final LearningUnitRepository learningUnitRepository;
    private final QuizChallengeItemRepository quizChallengeItemRepository;
    private final RewardCatalogRepository rewardCatalogRepository;
    private final AccountRepository accountRepository;
    private final AccountRewardRepository accountRewardRepository;
    private final AccountLearningUnitRepository accountLearningUnitRepository;
    private final ObjectMapper objectMapper;

    // ==========================================
    // CRUD Operations
    // ==========================================

    @Transactional
    public LearningUnit createQuiz(QuizCreateRequest request) {
        LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        if (!"LEVEL".equalsIgnoreCase(level.getType())) {
            throw new ApiException("INVALID_PARENT", "Parent phải là Level");
        }

        LearningUnit quiz = LearningUnit.builder()
                .parent(level)
                .name(request.getTitle())
                .type("QUIZ")
                .build();

        Map<String, Object> metadata = buildQuizMetadata(request);
        try {
            quiz.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            throw new ApiException("INVALID_METADATA", "Quiz metadata không hợp lệ");
        }

        // Gắn reward nếu có
        attachRewardToQuiz(quiz, request.getRewardCatalogId());

        return learningUnitRepository.save(quiz);
    }

    @Transactional
    public LearningUnit updateQuiz(UUID id, QuizCreateRequest request) {
        LearningUnit quiz = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Quiz"));

        if (!"QUIZ".equalsIgnoreCase(quiz.getType())) {
            throw new ApiException("INVALID_TYPE", "Đơn vị học tập không phải là Quiz");
        }

        quiz.setName(request.getTitle());

        Map<String, Object> metadata = buildQuizMetadata(request);
        try {
            quiz.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            throw new ApiException("INVALID_METADATA", "Quiz metadata không hợp lệ");
        }

        // Cập nhật reward (có thể null để xóa reward)
        attachRewardToQuiz(quiz, request.getRewardCatalogId());

        return learningUnitRepository.save(quiz);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllQuizzes() {
        return learningUnitRepository.findByType("QUIZ").stream()
                .map(this::buildQuizResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQuizzesByLevel(UUID levelId) {
        LearningUnit level = learningUnitRepository.findById(levelId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        if (!"LEVEL".equalsIgnoreCase(level.getType())) {
            throw new ApiException("INVALID_PARENT", "Parent phải là Level");
        }

        return learningUnitRepository.findByParentAndType(level, "QUIZ").stream()
                .map(this::buildQuizResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQuizzesByChallengeId(UUID challengeId) {
        List<org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem> quizChallengeItems =
                quizChallengeItemRepository.findByChallengeIdOrderByOrderIndex(challengeId);

        if (quizChallengeItems.isEmpty()) {
            return List.of();
        }

        List<UUID> quizIds = quizChallengeItems.stream()
                .map(org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem::getQuizId)
                .distinct()
                .toList();

        return learningUnitRepository.findAllById(quizIds).stream()
                .filter(quiz -> "QUIZ".equalsIgnoreCase(quiz.getType()))
                .map(this::buildQuizResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getQuizDetails(UUID id) {
        LearningUnit quiz = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Quiz"));

        if (!"QUIZ".equalsIgnoreCase(quiz.getType())) {
            throw new ApiException("INVALID_TYPE", "Đơn vị học tập không phải là Quiz");
        }

        return buildQuizResponse(quiz);
    }

    @Transactional
    public void deleteQuiz(UUID id) {
        LearningUnit quiz = learningUnitRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Quiz"));

        if (!"QUIZ".equalsIgnoreCase(quiz.getType())) {
            throw new ApiException("INVALID_TYPE", "Đơn vị học tập không phải là Quiz");
        }

        learningUnitRepository.delete(quiz);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQuizzesByChallenge(UUID challengeId) {
        List<UUID> quizIds = quizChallengeItemRepository.findByChallengeId(challengeId)
                .stream()
                .map(item -> item.getQuizId())
                .distinct()
                .collect(Collectors.toList());

        return learningUnitRepository.findAllById(quizIds).stream()
                .filter(u -> "QUIZ".equalsIgnoreCase(u.getType()))
                .map(this::buildQuizResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // Quiz Completion + Auto Reward Granting
    // ==========================================

    @Transactional
    public QuizCompleteResponse completeQuiz(UUID quizId, QuizCompleteRequest request, String userEmail) {
        // 1. Tìm quiz
        LearningUnit quiz = learningUnitRepository.findById(quizId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Quiz"));

        if (!"QUIZ".equalsIgnoreCase(quiz.getType())) {
            throw new ApiException("INVALID_TYPE", "Đơn vị học tập không phải là Quiz");
        }

        // 2. Tìm account
        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        // 3. Lấy passing_score từ metadata
        int passingScore = getPassingScoreFromMetadata(quiz);
        boolean passed = request.getScore() >= passingScore;

        // 4. Tính stars (1-3 sao)
        int stars = calculateStars(request.getScore(), passingScore);

        // 5. Lưu progress vào AccountLearningUnit
        saveProgress(account, quiz, stars, passed, request.getScore());

        // 6. Nếu ĐẠT và quiz có gắn reward → trao reward tự động
        RewardResponse earnedReward = null;
        boolean alreadyEarned = false;

        if (passed && quiz.getRewardCatalog() != null) {
            RewardCatalog reward = quiz.getRewardCatalog();
            Optional<AccountReward> existing = accountRewardRepository
                    .findByAccountIdAndRewardCatalogId(account.getId(), reward.getId());

            if (existing.isEmpty()) {
                // Tạo mới record → UNLOCKED
                AccountReward ar = AccountReward.builder()
                        .account(account)
                        .rewardCatalog(reward)
                        .status("UNLOCKED")
                        .progressValue(100)
                        .unlockedAt(Instant.now())
                        .build();
                accountRewardRepository.save(ar);
                earnedReward = RewardResponse.fromEntity(reward);
                log.info("Reward '{}' granted to user '{}' after completing quiz '{}'",
                        reward.getName(), userEmail, quiz.getName());
            } else {
                alreadyEarned = true;
                log.info("User '{}' already has reward '{}', skipping", userEmail, reward.getName());
            }
        }

        return QuizCompleteResponse.builder()
                .passed(passed)
                .score(request.getScore())
                .passingScore(passingScore)
                .starsEarned(stars)
                .earnedReward(earnedReward)
                .rewardAlreadyEarned(alreadyEarned)
                .build();
    }

    // ==========================================
    // User Progress & Rewards Retrieval
    // ==========================================

    /**
     * Lấy tiến trình quiz trong 1 level cho user hiện tại.
     * Trả về danh sách quiz, trạng thái hoàn thành, điểm cao nhất, sao, reward đã nhận chưa.
     */
    @Transactional(readOnly = true)
    public LevelProgressResponse getLevelProgress(UUID levelId, String userEmail) {
        // 1. Tìm level
        LearningUnit level = learningUnitRepository.findById(levelId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        if (!"LEVEL".equalsIgnoreCase(level.getType())) {
            throw new ApiException("INVALID_TYPE", "Đơn vị học tập không phải là Level");
        }

        // 2. Tìm account
        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        // 3. Lấy tất cả quiz trong level
        List<LearningUnit> quizzes = learningUnitRepository.findByParentAndType(level, "QUIZ");

        // 4. Lấy progress của user cho tất cả quiz
        List<AccountLearningUnit> progressList = accountLearningUnitRepository.findByAccountId(account.getId());
        Map<UUID, AccountLearningUnit> progressMap = progressList.stream()
                .collect(Collectors.toMap(
                        al -> al.getLearningUnit().getId(),
                        al -> al,
                        (existing, replacement) -> existing
                ));

        // 5. Lấy rewards đã nhận
        List<AccountReward> userRewards = accountRewardRepository.findByAccountId(account.getId());
        java.util.Set<UUID> earnedRewardIds = userRewards.stream()
                .filter(ar -> "UNLOCKED".equals(ar.getStatus()))
                .map(ar -> ar.getRewardCatalog().getId())
                .collect(Collectors.toSet());

        // 6. Build response
        int completedCount = 0;
        List<LevelProgressResponse.QuizProgressItem> quizItems = new java.util.ArrayList<>();

        for (LearningUnit quiz : quizzes) {
            AccountLearningUnit progress = progressMap.get(quiz.getId());
            boolean completed = progress != null && Boolean.TRUE.equals(progress.getIsCompleted());
            if (completed) completedCount++;

            RewardCatalog reward = quiz.getRewardCatalog();
            boolean rewardEarned = reward != null && earnedRewardIds.contains(reward.getId());

            LevelProgressResponse.QuizProgressItem item = LevelProgressResponse.QuizProgressItem.builder()
                    .quizId(quiz.getId())
                    .quizName(quiz.getName())
                    .passingScore(getPassingScoreFromMetadata(quiz))
                    .completed(completed)
                    .highestScore(progress != null && progress.getHighestScore() != null
                            ? progress.getHighestScore().intValue() : null)
                    .starsEarned(progress != null ? progress.getStarsEarned() : 0)
                    .rewardName(reward != null ? reward.getName() : null)
                    .rewardIconUrl(reward != null ? reward.getIconUrl() : null)
                    .rewardCatalogId(reward != null ? reward.getId() : null)
                    .rewardEarned(rewardEarned)
                    .build();

            quizItems.add(item);
        }

        return LevelProgressResponse.builder()
                .levelId(level.getId())
                .levelName(level.getName())
                .totalQuizzes(quizzes.size())
                .completedQuizzes(completedCount)
                .levelCompleted(quizzes.size() > 0 && completedCount == quizzes.size())
                .quizzes(quizItems)
                .build();
    }

    /**
     * Lấy tất cả thành tựu (rewards) của user hiện tại.
     */
    @Transactional(readOnly = true)
    public List<AccountBadgeResponse> getUserRewards(String userEmail) {
        Account account = accountRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        return accountRewardRepository.findByAccountId(account.getId())
                .stream()
                .filter(ar -> "UNLOCKED".equals(ar.getStatus()))
                .map(AccountBadgeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    /**
     * Gắn hoặc bỏ reward cho quiz.
     */
    private void attachRewardToQuiz(LearningUnit quiz, UUID rewardCatalogId) {
        if (rewardCatalogId != null) {
            RewardCatalog reward = rewardCatalogRepository.findById(rewardCatalogId)
                    .orElseThrow(() -> new ApiException("NOT_FOUND",
                            "Không tìm thấy thành tựu với ID: " + rewardCatalogId));
            quiz.setRewardCatalog(reward);
        } else {
            quiz.setRewardCatalog(null);
        }
    }

    /**
     * Lấy passing_score từ quiz metadata JSON.
     */
    private int getPassingScoreFromMetadata(LearningUnit quiz) {
        if (quiz.getMetadataJson() == null) return 70; // default
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                    quiz.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
            Object ps = metadata.get("passing_score");
            if (ps instanceof Number) return ((Number) ps).intValue();
            if (ps instanceof String) return Integer.parseInt((String) ps);
        } catch (Exception e) {
            log.warn("Cannot parse passing_score from quiz metadata, using default 70");
        }
        return 70;
    }

    /**
     * Tính số sao dựa trên score và passing_score.
     * 1 sao: >= passing_score
     * 2 sao: >= passing_score + (100 - passing_score) * 0.5
     * 3 sao: >= 90% hoặc full score
     */
    private int calculateStars(int score, int passingScore) {
        if (score < passingScore) return 0;
        if (score >= 90) return 3;
        int range = 100 - passingScore;
        if (range > 0 && score >= passingScore + range * 0.6) return 2;
        return 1;
    }

    /**
     * Lưu / cập nhật tiến trình người chơi cho quiz này.
     */
    private void saveProgress(Account account, LearningUnit quiz, int stars, boolean passed, int score) {
        Optional<AccountLearningUnit> existingOpt =
                accountLearningUnitRepository.findByAccountIdAndLearningUnitId(account.getId(), quiz.getId());

        AccountLearningUnit progress;
        if (existingOpt.isPresent()) {
            progress = existingOpt.get();
            // Chỉ cập nhật nếu score/stars mới cao hơn
            if (stars > progress.getStarsEarned()) {
                progress.setStarsEarned(stars);
            }
            if (passed && !progress.getIsCompleted()) {
                progress.setIsCompleted(true);
            }
            BigDecimal newScore = BigDecimal.valueOf(score);
            if (progress.getHighestScore() == null || newScore.compareTo(progress.getHighestScore()) > 0) {
                progress.setHighestScore(newScore);
            }
        } else {
            progress = AccountLearningUnit.builder()
                    .account(account)
                    .learningUnit(quiz)
                    .starsEarned(stars)
                    .isCompleted(passed)
                    .highestScore(BigDecimal.valueOf(score))
                    .build();
        }
        accountLearningUnitRepository.save(progress);
    }

    private Map<String, Object> buildQuizMetadata(QuizCreateRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("description", request.getDescription());
        metadata.put("instructions", request.getInstructions());
        metadata.put("time_limit_minutes", request.getTimeLimitMinutes());
        metadata.put("passing_score", request.getPassingScore());
        metadata.put("points_per_question", request.getPointsPerQuestion());
        metadata.put("difficulty", request.getDifficulty());
        metadata.put("questions", request.getQuestions());
        metadata.put("question_count", request.getQuestionCount());
        metadata.put("comment", request.getComment());
        metadata.put("skill_type", request.getSkillType());
        return metadata;
    }

    private Map<String, Object> buildQuizResponse(LearningUnit quiz) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", quiz.getId());
        response.put("levelId", quiz.getParent() != null ? quiz.getParent().getId() : null);
        response.put("name", quiz.getName());
        response.put("type", quiz.getType());

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (quiz.getMetadataJson() != null) {
            try {
                metadata = objectMapper.readValue(quiz.getMetadataJson(), new TypeReference<Map<String, Object>>() {
                });
            } catch (JsonProcessingException e) {
                throw new ApiException("INVALID_METADATA", "Quiz metadata không hợp lệ");
            }
        }

        response.put("description", metadata.get("description"));
        response.put("instructions", metadata.get("instructions"));
        response.put("timeLimitMinutes", metadata.get("time_limit_minutes"));
        response.put("passingScore", metadata.get("passing_score"));
        response.put("pointsPerQuestion", metadata.get("points_per_question"));
        response.put("difficulty", metadata.get("difficulty"));
        response.put("skillType", metadata.get("skill_type"));

        List<QuizQuestionRequest> questions = metadata.containsKey("questions")
                ? objectMapper.convertValue(metadata.get("questions"), new TypeReference<List<QuizQuestionRequest>>() {
                })
                : List.of();
        response.put("questions", questions);
        response.put("questionCount", metadata.get("question_count"));
        response.put("comment", metadata.get("comment"));
        response.put("skillType", metadata.get("skill_type"));

        // Thêm thông tin reward
        RewardCatalog reward = quiz.getRewardCatalog();
        if (reward != null) {
            response.put("rewardCatalogId", reward.getId());
            response.put("rewardName", reward.getName());
            response.put("rewardIconUrl", reward.getIconUrl());
        } else {
            response.put("rewardCatalogId", null);
            response.put("rewardName", null);
            response.put("rewardIconUrl", null);
        }

        return response;
    }
}
