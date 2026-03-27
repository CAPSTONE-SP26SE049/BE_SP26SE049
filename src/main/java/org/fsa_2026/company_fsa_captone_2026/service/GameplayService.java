package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fsa_2026.company_fsa_captone_2026.dto.AttemptRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.AttemptResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.PhonemeFeedbackResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.PracticeSessionResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.*;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GameplayService
 * Đã cập nhật: dùng StudySession + SessionDetail thay cho PracticeSession + Attempt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameplayService {

    private final AccountRepository accountRepository;
    private final StudySessionRepository studySessionRepository;
    private final SessionDetailRepository sessionDetailRepository;
    private final ContentItemRepository contentItemRepository;
    private final LearningUnitRepository learningUnitRepository;
    private final QuizChallengeItemRepository quizChallengeItemRepository;
    private final AccountLearningUnitRepository accountLearningUnitRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PracticeSessionResponse startSession(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        StudySession session = StudySession.builder()
                .account(account)
                .sessionType("PRACTICE")
                .startedAt(Instant.now())
                .build();

        session = studySessionRepository.save(session);
        return PracticeSessionResponse.fromEntity(session);
    }

    @Transactional
    public PracticeSessionResponse endSession(String email, String sessionId) {
        StudySession session = studySessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Session not found"));

        if (!session.getAccount().getEmail().equals(email)) {
            throw new ApiException("FORBIDDEN", "Session does not belong to this account");
        }

        session.setEndedAt(Instant.now());
        session = studySessionRepository.save(session);
        return PracticeSessionResponse.fromEntity(session);
    }

    @Transactional
    public AttemptResponse submitAttempt(String email, AttemptRequest request) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        StudySession session = null;
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            session = studySessionRepository.findById(UUID.fromString(request.getSessionId()))
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Session not found"));
        }

        ContentItem challenge = contentItemRepository.findById(UUID.fromString(request.getChallengeId()))
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Challenge not found"));

        double randomScoreDouble = 50 + (Math.random() * 50);
        BigDecimal scoreOverall = BigDecimal.valueOf(randomScoreDouble);
        boolean isPassed = (request.getIsPassed() != null) ? request.getIsPassed() : (randomScoreDouble >= 80.0);
        int latencyMs = (int) (Math.random() * 500) + 100;

        // Build phoneme feedback từ metadata của challenge
        List<PhonemeFeedbackDetail> feedbackList = new ArrayList<>();
        try {
            if (challenge.getMetadataJson() != null) {
                Map<?, ?> meta = objectMapper.readValue(challenge.getMetadataJson(), Map.class);
                String focusPhonemes = (String) meta.get("focus_phonemes");
                if (focusPhonemes != null && !focusPhonemes.isEmpty()) {
                    String[] phonemes = focusPhonemes.split(",");
                    int order = 1;
                    for (String p : phonemes) {
                        feedbackList.add(PhonemeFeedbackDetail.builder()
                                .sequenceOrder(order)
                                .phonemeIpa(p.trim())
                                .score(BigDecimal.valueOf(60 + (Math.random() * 40)))
                                .startTimeMs((order - 1) * 500)
                                .endTimeMs(order * 500)
                                .build());
                        order++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse challenge metadata for phoneme feedback", e);
        }

        // Lưu toàn bộ attempt context vào attemptMetadataJson
        String attemptMetadataJson = null;
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("audioUrl", request.getAudioUrl());
            meta.put("latencyMs", latencyMs);
            if (!feedbackList.isEmpty()) {
                meta.put("phonemeFeedback", feedbackList);
            }
            attemptMetadataJson = objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            log.error("Failed to serialize attempt metadata", e);
        }

        SessionDetail detail = SessionDetail.builder()
                .session(session)
                .contentItem(challenge)
                .isPassed(isPassed)
                .scoreOverall(scoreOverall)
                .attemptMetadataJson(attemptMetadataJson)
                .build();

        detail = sessionDetailRepository.save(detail);

        if (isPassed) {
            account.setTotalStars(account.getTotalStars() + 3);
            account.setTotalExperience(account.getTotalExperience() + 10);
            accountRepository.save(account);

            // Cập nhật tiến độ Quiz và Level
            updateQuizAndLevelProgress(account, challenge);
        }

        AttemptResponse response = AttemptResponse.fromEntity(detail);
        response.setFeedback(
                feedbackList.stream().map(PhonemeFeedbackResponse::fromDetail).collect(Collectors.toList()));

        return response;
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> getAttemptHistory(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        return sessionDetailRepository.findByAccountIdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(AttemptResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void updateQuizAndLevelProgress(Account account, ContentItem challenge) {
        // 1. Tìm tất cả các Quiz (LearningUnit) chứa Challenge này
        // Cả hệ thống mới (QuizChallengeItem) và cũ (learning_unit_id gắn trực tiếp)
        List<UUID> quizIds = quizChallengeItemRepository.findByChallengeId(challenge.getId())
                .stream()
                .map(QuizChallengeItem::getQuizId)
                .distinct()
                .collect(Collectors.toList());

        // Nếu challenge này thuộc về một LearningUnit kiểu QUIZ (hệ thống cũ)
        if (challenge.getLearningUnit() != null && "QUIZ".equalsIgnoreCase(challenge.getLearningUnit().getType())) {
            UUID luQuizId = challenge.getLearningUnit().getId();
            if (!quizIds.contains(luQuizId)) {
                quizIds.add(luQuizId);
            }
        }

        for (UUID quizId : quizIds) {
            LearningUnit quiz = learningUnitRepository.findById(quizId).orElse(null);
            if (quiz == null) continue;

            // Cập nhật AccountLearningUnit cho Quiz
            AccountLearningUnit quizProgress = accountLearningUnitRepository
                    .findByAccountIdAndLearningUnitId(account.getId(), quizId)
                    .orElseGet(() -> AccountLearningUnit.builder()
                            .account(account)
                            .learningUnit(quiz)
                            .isCompleted(false)
                            .starsEarned(0)
                            .build());

            if (!quizProgress.getIsCompleted() || quizProgress.getHighestScore() == null) {
                // Xác định danh sách câu hỏi trong Quiz này
                List<UUID> questionChallengeIds = new ArrayList<>();
                List<QuizChallengeItem> quizItems = quizChallengeItemRepository.findByQuizIdOrderByOrderIndex(quizId);
                
                if (!quizItems.isEmpty()) {
                    // Hệ thống mới
                    questionChallengeIds = quizItems.stream().map(QuizChallengeItem::getChallengeId).collect(Collectors.toList());
                } else {
                    // Hệ thống cũ (LearningUnit Quiz)
                    questionChallengeIds = contentItemRepository.findByLearningUnitId(quizId)
                            .stream()
                            .filter(ci -> !"QUIZ".equalsIgnoreCase(ci.getType())) // chỉ lấy các challenge thực tế (LISTENING, SPEAKING...)
                            .map(ContentItem::getId)
                            .collect(Collectors.toList());
                }

                if (questionChallengeIds.isEmpty()) continue;

                // Kiểm tra xem đã hoàn thành bao nhiêu câu hỏi trong Quiz
                long passedCount = questionChallengeIds.stream()
                        .filter(cid -> sessionDetailRepository.existsByContentItemIdAndSessionAccountIdAndIsPassed(
                                cid, account.getId(), true))
                        .count();

                double percentage = (double) passedCount / questionChallengeIds.size() * 100;
                log.info("Checking progress for quiz {}: {}/{} questions passed ({}%)", quiz.getName(), passedCount, questionChallengeIds.size(), percentage);

                // Cập nhật điểm cao nhất
                if (quizProgress.getHighestScore() == null || BigDecimal.valueOf(percentage).compareTo(quizProgress.getHighestScore()) > 0) {
                    quizProgress.setHighestScore(BigDecimal.valueOf(percentage));
                }

                if (percentage >= 80.0) { // Mặc định 80% là qua
                    quizProgress.setIsCompleted(true);
                    
                    // Tính sao: 80-89: 1 sao, 90-99: 2 sao, 100: 3 sao
                    int stars = 1;
                    if (percentage >= 100.0) stars = 3;
                    else if (percentage >= 90.0) stars = 2;
                    
                    if (stars > quizProgress.getStarsEarned()) {
                        quizProgress.setStarsEarned(stars);
                    }
                    
                    accountLearningUnitRepository.save(quizProgress);
                    log.info("Quiz {} marked as completed ({}%) for user {}", quiz.getName(), percentage, account.getEmail());

                    // 2. Sau khi Quiz xong, kiểm tra Level (Parent của Quiz)
                    if (quiz.getParent() != null && "LEVEL".equalsIgnoreCase(quiz.getParent().getType())) {
                        checkAndMarkLevelCompletion(account, quiz.getParent());
                    }
                } else {
                    accountLearningUnitRepository.save(quizProgress);
                }
            }
        }

        // 3. Nếu Challenge thuộc trực tiếp về một Level (không qua Quiz)
        if (challenge.getLearningUnit() != null && "LEVEL".equalsIgnoreCase(challenge.getLearningUnit().getType())) {
            checkAndMarkLevelCompletion(account, challenge.getLearningUnit());
        }
    }

    private void checkAndMarkLevelCompletion(Account account, LearningUnit level) {
        // Lấy tất cả quiz thuộc level này - cả 2 hệ thống
        List<LearningUnit> legacyQuizzes = learningUnitRepository.findByParentIdAndType(level.getId(), "QUIZ");
        List<ContentItem> modernQuizzes = contentItemRepository.findByLearningUnitIdAndType(level.getId(), "QUIZ");

        List<UUID> allQuizIds = new ArrayList<>();
        legacyQuizzes.forEach(q -> allQuizIds.add(q.getId()));
        modernQuizzes.forEach(q -> allQuizIds.add(q.getId()));

        if (allQuizIds.isEmpty()) return;

        boolean allCompleted = true;
        int totalStars = 0;

        for (UUID quizId : allQuizIds) {
            AccountLearningUnit progress = accountLearningUnitRepository
                    .findByAccountIdAndLearningUnitId(account.getId(), quizId)
                    .orElse(null);
            
            if (progress == null || !progress.getIsCompleted()) {
                allCompleted = false;
                break;
            }
            totalStars += progress.getStarsEarned();
        }

        if (allCompleted) {
            AccountLearningUnit levelProgress = accountLearningUnitRepository
                    .findByAccountIdAndLearningUnitId(account.getId(), level.getId())
                    .orElseGet(() -> AccountLearningUnit.builder()
                            .account(account)
                            .learningUnit(level)
                            .isCompleted(false)
                            .starsEarned(0)
                            .build());

            levelProgress.setIsCompleted(true);
            levelProgress.setStarsEarned(totalStars / allQuizIds.size());
            accountLearningUnitRepository.save(levelProgress);
            log.info("Level {} marked as completed for user {}", level.getName(), account.getEmail());
        }
    }

    /**
     * Mark a quiz as complete based on score.
     * quizId có thể là ContentItem.id (modern) hoặc LearningUnit.id (legacy).
     * Frontend gọi API này sau khi người dùng trả lời xong tất cả câu hỏi.
     */
    @Transactional
    public Map<String, Object> markQuizComplete(String email, UUID quizId, int correctCount, int totalCount) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        double percentage = totalCount > 0 ? (double) correctCount / totalCount * 100 : 0;
        boolean passed = percentage >= 80.0;
        int stars = passed ? (percentage >= 100 ? 3 : percentage >= 90 ? 2 : 1) : 0;

        // Try as ContentItem (modern quiz)
        ContentItem modernQuiz = contentItemRepository.findById(quizId).orElse(null);
        if (modernQuiz != null && "QUIZ".equalsIgnoreCase(modernQuiz.getType())) {
            // Ensure there is a corresponding LearningUnit with the exact same ID
            // so we can use the AccountLearningUnit progress table
            LearningUnit quizLU = learningUnitRepository.findById(quizId).orElseGet(() -> {
                LearningUnit lu = LearningUnit.builder()
                        .name(modernQuiz.getTitle())
                        .type("QUIZ")
                        .parent(modernQuiz.getLearningUnit())
                        .build();
                // We enforce the SAME ID so AccountLearningUnit lookup works via quizId
                lu.setId(quizId);
                return learningUnitRepository.save(lu);
            });

            AccountLearningUnit progress = accountLearningUnitRepository
                    .findByAccountIdAndLearningUnitId(account.getId(), quizLU.getId())
                    .orElseGet(() -> AccountLearningUnit.builder()
                        .account(account).learningUnit(quizLU)
                        .isCompleted(false).starsEarned(0).build());

            if (passed) {
                if (!progress.getIsCompleted()) progress.setIsCompleted(true);
                if (stars > progress.getStarsEarned()) progress.setStarsEarned(stars);
            }
            if (progress.getHighestScore() == null || BigDecimal.valueOf(percentage).compareTo(progress.getHighestScore()) > 0) {
                progress.setHighestScore(BigDecimal.valueOf(percentage));
            }
            accountLearningUnitRepository.save(progress);

            // Trigger level completion check if applicable
            if (passed && quizLU.getParent() != null) {
                checkAndMarkLevelCompletion(account, quizLU.getParent());
            }

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("passed", passed);
            result.put("percentage", Math.round(percentage));
            result.put("stars", stars);
            return result;
        }

        // Try as LearningUnit (legacy quiz)
        LearningUnit legacyQuiz = learningUnitRepository.findById(quizId).orElse(null);
        if (legacyQuiz != null && "QUIZ".equalsIgnoreCase(legacyQuiz.getType())) {
            AccountLearningUnit progress = accountLearningUnitRepository
                    .findByAccountIdAndLearningUnitId(account.getId(), quizId)
                    .orElseGet(() -> AccountLearningUnit.builder()
                            .account(account).learningUnit(legacyQuiz)
                            .isCompleted(false).starsEarned(0).build());

            if (passed) {
                if (!progress.getIsCompleted()) progress.setIsCompleted(true);
                if (stars > progress.getStarsEarned()) progress.setStarsEarned(stars);
            }
            if (progress.getHighestScore() == null || BigDecimal.valueOf(percentage).compareTo(progress.getHighestScore()) > 0) {
                progress.setHighestScore(BigDecimal.valueOf(percentage));
            }
            accountLearningUnitRepository.save(progress);

            if (passed && legacyQuiz.getParent() != null) {
                checkAndMarkLevelCompletion(account, legacyQuiz.getParent());
            }
        }

        log.info("markQuizComplete for user {} quizId {} -> passed={} ({}%)", email, quizId, passed, Math.round(percentage));

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("passed", passed);
        result.put("percentage", Math.round(percentage));
        result.put("stars", stars);
        return result;
    }
}
