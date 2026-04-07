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
import org.fsa_2026.company_fsa_captone_2026.dto.UserRegionProgressResponse;
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

        // Tính toán orderIndex tự động nếu trống
        if (request.getOrderIndex() == null) {
            // Sử dụng ID để tránh các vấn đề liên quan đến Hibernate Proxy/Persistence Context
            List<LearningUnit> existingQuizzes = learningUnitRepository.findByParentIdAndType(level.getId(), "QUIZ");
            
            int maxOrder = existingQuizzes.stream()
                    .mapToInt(this::extractOrderIndexFromMetadata)
                    .max()
                    .orElse(0);
            request.setOrderIndex(maxOrder + 1);
        }

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

        // 4. Tính toán tỷ lệ phần trăm đúng
        int total = request.getTotalQuestions() != null ? request.getTotalQuestions() : 10;
        int correct = request.getCorrectAnswers() != null ? request.getCorrectAnswers() : 0;
        double percentage = (double) correct / total * 100.0;

        // 5. Tính số sao dựa trên phần trăm: >=40% (1 sao), >=60% (2 sao), >=80% (3 sao)
        int stars = calculateStarsBasedOnPercentage(percentage);

        // 6. Theo yêu cầu: Chỉ coi là hoàn thành (để mở quiz sau) nếu đạt >= 2 sao (tức >= 60%)
        boolean passed = stars >= 2;

        // 7. Lưu progress vào AccountLearningUnit (Lưu score là phần trăm)
        saveProgress(account, quiz, stars, passed, (int) Math.round(percentage));


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

        int percentageInt = (int) Math.round(percentage);
        return QuizCompleteResponse.builder()
                .passed(passed)
                .score(percentageInt)
                .passingScore(60) // Theo yêu cầu mới: Cần đạt 60% để tính là Pass (mở màn tiếp)
                .starsEarned(stars)
                .earnedReward(earnedReward)
                .rewardAlreadyEarned(alreadyEarned)
                .newTotalStars(account.getTotalStars() != null ? account.getTotalStars() : 0)
                .newTotalXP(account.getTotalExperience() != null ? account.getTotalExperience() : 0)
                .build();
    }




    // ==========================================
    // Quiz Scoring Recalculation (for admin/educator updates)
    // ==========================================

    /**
     * Recalculate quiz scoring metadata whenever question assignments change.
     *
     * total_points = question_count * points_per_question
     * passing_score_points = ceil(total_points * passing_score_percent / 100)
     */
    @Transactional
    public Map<String, Object> recalculateQuizScoring(UUID quizId) {
        LearningUnit quiz = learningUnitRepository.findById(quizId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Quiz"));

        if (!"QUIZ".equalsIgnoreCase(quiz.getType())) {
            throw new ApiException("INVALID_TYPE", "Đơn vị học tập không phải là Quiz");
        }

        int questionCount = (int) quizChallengeItemRepository.countByQuizId(quizId);
        int pointsPerQuestion = getPointsPerQuestionFromMetadata(quiz);
        int passingPercent = getPassingScoreFromMetadata(quiz);

        int totalPoints = questionCount * pointsPerQuestion;
        int passingScorePoints = (int) Math.ceil(totalPoints * (passingPercent / 100.0));

        Map<String, Object> metadata;
        try {
            metadata = quiz.getMetadataJson() != null
                    ? objectMapper.readValue(quiz.getMetadataJson(), new TypeReference<Map<String, Object>>() {})
                    : new LinkedHashMap<>();
        } catch (Exception e) {
            metadata = new LinkedHashMap<>();
        }

        metadata.put("question_count", questionCount);
        metadata.put("points_per_question", pointsPerQuestion);
        metadata.put("total_points", totalPoints);
        metadata.put("passing_score", passingPercent);
        metadata.put("passing_score_points", passingScorePoints);

        try {
            quiz.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            throw new ApiException("INVALID_METADATA", "Quiz metadata không hợp lệ");
        }
        learningUnitRepository.save(quiz);

        Map<String, Object> scoring = new LinkedHashMap<>();
        scoring.put("quizId", quizId);
        scoring.put("questionCount", questionCount);
        scoring.put("pointsPerQuestion", pointsPerQuestion);
        scoring.put("totalPoints", totalPoints);
        scoring.put("passingScorePercent", passingPercent);
        scoring.put("passingScorePoints", passingScorePoints);
        scoring.put("starRule", Map.of("oneStar", 40, "twoStars", 60, "threeStars", 80));

        return scoring;
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

        // 3. Lấy tất cả quiz trong level và sắp xếp theo orderIndex
        List<LearningUnit> quizzes = learningUnitRepository.findByParentAndType(level, "QUIZ");
        quizzes.sort(java.util.Comparator.comparingInt(this::extractOrderIndexFromMetadata));

        // 4. Lấy progress của user cho tất cả quiz
        List<AccountLearningUnit> progressList = accountLearningUnitRepository.findByAccountId(account.getId());
        Map<UUID, AccountLearningUnit> progressMap = progressList.stream()
                .collect(Collectors.toMap(
                        al -> al.getLearningUnit().getId(),
                        al -> al,
                        (existing, replacement) -> existing
                ));

        // 5. Lấy rewards đã nhận (AccountReward)
        List<AccountReward> userRewards = accountRewardRepository.findByAccountId(account.getId());
        log.info("Checking rewards for user {}: found {} records in account_reward", userEmail, userRewards.size());
        
        java.util.Set<UUID> earnedRewardIds = userRewards.stream()
                .filter(ar -> "UNLOCKED".equalsIgnoreCase(ar.getStatus()))
                .map(ar -> ar.getRewardCatalog().getId())
                .collect(Collectors.toSet());
        
        if (!earnedRewardIds.isEmpty()) {
            log.info("User {} has earned {} unique reward IDs: {}", userEmail, earnedRewardIds.size(), earnedRewardIds);
        }

        // 6. Build response
        int completedCount = 0;
        List<LevelProgressResponse.QuizProgressItem> quizItems = new java.util.ArrayList<>();

        for (LearningUnit quiz : quizzes) {
            AccountLearningUnit progress = progressMap.get(quiz.getId());
            boolean completed = progress != null && Boolean.TRUE.equals(progress.getIsCompleted());
            if (completed) completedCount++;

            RewardCatalog reward = quiz.getRewardCatalog();
            boolean rewardEarned = false;
            
            if (reward != null) {
                rewardEarned = earnedRewardIds.contains(reward.getId());
                log.debug("Quiz '{}' ({}): Reward '{}' (ID={}) earned? {}", 
                    quiz.getName(), quiz.getId(), reward.getName(), reward.getId(), rewardEarned);
            }

            LevelProgressResponse.QuizProgressItem item = LevelProgressResponse.QuizProgressItem.builder()
                    .quizId(quiz.getId())
                    .quizName(quiz.getName())
                    .orderIndex(extractOrderIndexFromMetadata(quiz))
                    .passingScore(getPassingScoreFromMetadata(quiz))
                    .completed(completed)
                    .highestScore(progress != null && progress.getHighestScore() != null
                            ? progress.getHighestScore().intValue() : null)
                    .starsEarned(progress != null ? progress.getStarsEarned() : 0)
                    .rewardName(reward != null ? reward.getName() : null)
                    .rewardIconUrl(reward != null ? reward.getIconUrl() : null)
                    .rewardCatalogId(reward != null ? reward.getId() : null)
                    .rewardEarned(rewardEarned)
                    .skillType(getSkillTypeFromMetadata(quiz))
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
     * Validation: 1 reward chỉ được gắn cho 1 quiz duy nhất.
     */
    private void attachRewardToQuiz(LearningUnit quiz, UUID rewardCatalogId) {
        if (rewardCatalogId != null) {
            RewardCatalog reward = rewardCatalogRepository.findById(rewardCatalogId)
                    .orElseThrow(() -> new ApiException("NOT_FOUND",
                            "Không tìm thấy thành tựu với ID: " + rewardCatalogId));

            // Check if this reward is already linked to another quiz
            Optional<LearningUnit> existingQuiz;
            if (quiz.getId() != null) {
                // Update case: exclude current quiz from check
                existingQuiz = learningUnitRepository.findByRewardCatalogIdAndIdNot(rewardCatalogId, quiz.getId());
            } else {
                // Create case: check all quizzes
                existingQuiz = learningUnitRepository.findByRewardCatalogId(rewardCatalogId);
            }

            if (existingQuiz.isPresent()) {
                throw new ApiException("BAD_REQUEST",
                        "Thành tựu '" + reward.getName() + "' đã được gắn cho quiz '"
                                + existingQuiz.get().getName() + "'. Mỗi thành tựu chỉ được gắn cho 1 quiz.");
            }

            quiz.setRewardCatalog(reward);
        } else {
            quiz.setRewardCatalog(null);
        }
    }

    private String getSkillTypeFromMetadata(LearningUnit quiz) {
        if (quiz.getMetadataJson() == null) return "MIXED";
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                    quiz.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
            return (String) metadata.getOrDefault("skill_type", "MIXED");
        } catch (Exception e) {
            log.warn("Cannot parse skill_type from quiz metadata, using default MIXED");
            return "MIXED";
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

    private int getPointsPerQuestionFromMetadata(LearningUnit quiz) {
        if (quiz.getMetadataJson() == null) return 10; // default
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                    quiz.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
            Object ppq = metadata.get("points_per_question");
            if (ppq instanceof Number) return ((Number) ppq).intValue();
            if (ppq instanceof String) return Integer.parseInt((String) ppq);
        } catch (Exception e) {
            log.warn("Cannot parse points_per_question from quiz metadata, using default 10");
        }
        return 10;
    }

    /**
     * Tính số sao theo yêu cầu mới:
     * - >= 80%: 3 sao
     * - >= 60%: 2 sao
     * - >= 40%: 1 sao
     * - < 40%: 0 sao
     */
    private int calculateStarsBasedOnPercentage(double percentage) {
        if (percentage >= 80) return 3;
        if (percentage >= 60) return 2;
        if (percentage >= 40) return 1;
        return 0;
    }


    /**
     * Lấy tóm tắt tiến trình theo từng vùng miền (MIEN_BAC, MIEN_TRUNG, MIEN_NAM).
     */
    @Transactional(readOnly = true)
    public UserRegionProgressResponse getMyProgressSummary(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Người dùng không tồn tại"));

        // Lấy tất cả dialects (levels cha)
        List<LearningUnit> dialects = learningUnitRepository.findByType("DIALECT");
        List<AccountLearningUnit> allProgress = accountLearningUnitRepository.findByAccountId(account.getId());
        
        java.util.Map<UUID, AccountLearningUnit> progressMap = allProgress.stream()
                .collect(Collectors.toMap(
                        al -> al.getLearningUnit().getId(),
                        al -> al,
                        (e, r) -> e
                ));

        List<UserRegionProgressResponse.RegionProgress> regionList = new java.util.ArrayList<>();

        for (LearningUnit dialect : dialects) {
            // Lấy tất cả các levels thuộc dialect này
            List<LearningUnit> levels = learningUnitRepository.findByParentId(dialect.getId());
            int totalStars = 0;
            int totalQuizzes = 0;
            int completedQuizzes = 0;

            for (LearningUnit level : levels) {
                List<LearningUnit> quizzes = learningUnitRepository.findByParentIdAndType(level.getId(), "QUIZ");
                totalQuizzes += quizzes.size();
                for (LearningUnit quiz : quizzes) {
                    AccountLearningUnit p = progressMap.get(quiz.getId());
                    if (p != null) {
                        totalStars += p.getStarsEarned();
                        if (Boolean.TRUE.equals(p.getIsCompleted())) {
                            completedQuizzes++;
                        }
                    }
                }
            }

            double percentage = totalQuizzes > 0 ? (double) completedQuizzes / totalQuizzes * 100 : 0;

            regionList.add(UserRegionProgressResponse.RegionProgress.builder()
                    .regionName(dialect.getName())
                    .totalStars(totalStars)
                    .totalQuizzes(totalQuizzes)
                    .completedQuizzes(completedQuizzes)
                    .completionPercentage(percentage)
                    .build());
        }

        return UserRegionProgressResponse.builder()
                .regions(regionList)
                .build();
    }

    /**
     * Lưu / cập nhật tiến trình người chơi cho quiz này.
     */
    private void saveProgress(Account account, LearningUnit quiz, int stars, boolean passed, int score) {
        Optional<AccountLearningUnit> existingOpt =
                accountLearningUnitRepository.findByAccountIdAndLearningUnitId(account.getId(), quiz.getId());

        AccountLearningUnit progress;
        int oldStars = 0;
        
        if (existingOpt.isPresent()) {
            progress = existingOpt.get();
            oldStars = progress.getStarsEarned() != null ? progress.getStarsEarned() : 0;
            
            // Update stars if higher
            if (stars > oldStars) {
                progress.setStarsEarned(stars);
            }
            // Update completion if passed
            if (passed) {
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
        
        // Update Account total stars & XP (only the delta if new score is higher)
        if (stars > oldStars) {
            int delta = stars - oldStars;
            
            int currentTotalStars = account.getTotalStars() != null ? account.getTotalStars() : 0;
            account.setTotalStars(currentTotalStars + delta);
            
            // Give 10 XP per NEW star
            int currentTotalXp = account.getTotalExperience() != null ? account.getTotalExperience() : 0;
            account.setTotalExperience(currentTotalXp + (delta * 10));
            
            accountRepository.save(account);
        }


        accountLearningUnitRepository.save(progress);
    }


    private Map<String, Object> buildQuizMetadata(QuizCreateRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("description", request.getDescription());
        metadata.put("instructions", request.getInstructions());
        metadata.put("time_limit_seconds", request.getTimeLimitSeconds());
        metadata.put("passing_score", request.getPassingScore());
        metadata.put("points_per_question", request.getPointsPerQuestion());
        metadata.put("difficulty", request.getDifficulty());
        metadata.put("questions", request.getQuestions());
        metadata.put("question_count", request.getQuestionCount());
        metadata.put("comment", request.getComment());
        metadata.put("skill_type", request.getSkillType());
        metadata.put("orderIndex", request.getOrderIndex());
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
                metadata = objectMapper.readValue(quiz.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse quiz metadata for {}: {}", quiz.getId(), e.getMessage());
            }
        }

        response.put("description", metadata.getOrDefault("description", ""));
        response.put("instructions", metadata.getOrDefault("instructions", ""));

        // Robust parsing for timeLimitSeconds
        Object tls = metadata.get("time_limit_seconds");
        Object tlm = metadata.get("time_limit_minutes");
        Integer timeLimitSeconds = 0;
        try {
            if (tls instanceof Number) timeLimitSeconds = ((Number) tls).intValue();
            else if (tlm instanceof Number) timeLimitSeconds = ((Number) tlm).intValue() * 60;
            else if (tls instanceof String) timeLimitSeconds = Integer.parseInt((String) tls);
            else if (tlm instanceof String) timeLimitSeconds = Integer.parseInt((String) tlm) * 60;
        } catch (Exception ignored) {}
        
        response.put("timeLimitSeconds", timeLimitSeconds);
        response.put("passingScore", metadata.getOrDefault("passing_score", 60));
        response.put("difficulty", metadata.getOrDefault("difficulty", "BEGINNER"));
        response.put("questionCount", metadata.getOrDefault("question_count", 0));
        response.put("orderIndex", metadata.getOrDefault("orderIndex", 0));
        response.put("skillType", metadata.getOrDefault("skill_type", "READING"));
        response.put("comment", metadata.getOrDefault("comment", ""));

        // Reward Info
        RewardCatalog reward = quiz.getRewardCatalog();
        if (reward != null) {
            response.put("rewardCatalogId", reward.getId());
            response.put("rewardName", reward.getName());
            response.put("rewardIconUrl", reward.getIconUrl());
        }

        return response;
    }

    private int extractOrderIndexFromMetadata(LearningUnit quiz) {
        if (quiz.getMetadataJson() == null || quiz.getMetadataJson().isBlank()) return 0;
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                    quiz.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
            Object oi = metadata.get("orderIndex");
            if (oi instanceof Number) return ((Number) oi).intValue();
            if (oi instanceof String) {
                try {
                    return Integer.parseInt((String) oi);
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

}
