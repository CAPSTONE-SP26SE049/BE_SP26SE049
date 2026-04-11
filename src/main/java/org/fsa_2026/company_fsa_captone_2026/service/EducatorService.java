package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.*;
import org.fsa_2026.company_fsa_captone_2026.entity.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EducatorService {

        private static final String CODE_NOT_FOUND = "NOT_FOUND";
        private static final String TYPE_LEVEL = "LEVEL";
        private static final String TYPE_PRONUNCIATION = "PRONUNCIATION";
        private static final String STATUS_APPROVED = "APPROVED";
        private static final String MSG_LEVEL_NOT_FOUND = "Không tìm thấy Level";

        private final AccountRepository accountRepository;
        private final SessionDetailRepository sessionDetailRepository;
        private final LearningUnitRepository learningUnitRepository;
        private final ContentItemRepository contentItemRepository;
        private final EducatorFeedbackRepository educatorFeedbackRepository;
        private final PlacementRuleRepository placementRuleRepository;
        private final StudentAssignmentRepository studentAssignmentRepository;
        private final AccountDashboardSummaryRepository dashboardSummaryRepository;
        private final LessonPlanRepository lessonPlanRepository;
        private final LessonPlanItemRepository lessonPlanItemRepository;
        private final QuestRepository questRepository;
        private final AccountQuestProgressRepository questProgressRepository;
        private final ContentApprovalHistoryRepository contentApprovalHistoryRepository;
        private final ObjectMapper objectMapper;

        // ─── Curriculum / Level ──────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<LevelResponse> getCurriculumByRegion(String region) {
                LearningUnit dialect = learningUnitRepository
                                .findByTypeAndNameIgnoreCase("DIALECT", region)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND,
                                                "Không tìm thấy vùng miền: " + region));

                return learningUnitRepository
                                .findByParentIdAndType(dialect.getId(), TYPE_LEVEL).stream()
                                .map(LevelResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional
        public LevelResponse createLevel(String educatorEmail, LevelCreateRequest request) {

                Account educator = getAccountByEmail(educatorEmail);

                LearningUnit parent = null;
                if (request.getParentId() != null) {
                        parent = learningUnitRepository.findById(request.getParentId())
                                        .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy parent"));
                }

                LearningUnit level;
                try {
                        Map<String, Object> metadata = request.getMetadataJson() != null
                                        ? new LinkedHashMap<>(request.getMetadataJson())
                                        : new LinkedHashMap<>();
                        metadata.put("status", STATUS_APPROVED);

                        level = LearningUnit.builder()
                                        .parent(parent)
                                        .name(request.getName())
                                        .type(request.getType())
                                        .metadataJson(objectMapper.writeValueAsString(metadata))
                                        .build();
                } catch (JsonProcessingException e) {
                        throw new ApiException("INTERNAL_ERROR", "Không thể tạo Learning Unit");
                }
                level.setCreatedBy(educator.getId().toString());
                level = learningUnitRepository.save(level);

                LevelResponse response = LevelResponse.fromEntity(level);
                saveApprovalHistory(TYPE_LEVEL, level.getId(), educator, ContentStatus.APPROVED,
                                request.getComment() != null && !request.getComment().isBlank()
                                                ? request.getComment()
                                                : "Educator created level (Auto-approved)",
                                response);
                return response;
        }

        @Transactional
        public LevelResponse updateLevel(String educatorEmail, UUID levelId, LevelCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
                LearningUnit level = learningUnitRepository.findById(levelId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));

                if (request.getParentId() != null
                                && (level.getParent() == null
                                                || !level.getParent().getId().equals(request.getParentId()))) {
                        LearningUnit parent = learningUnitRepository.findById(request.getParentId())
                                        .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy parent"));
                        level.setParent(parent);
                }

                level.setName(request.getName());
                level.setType(request.getType());

                try {
                        Map<String, Object> metadata = request.getMetadataJson() != null
                                        ? new LinkedHashMap<>(request.getMetadataJson())
                                        : new LinkedHashMap<>();
                        metadata.put("status", STATUS_APPROVED);
                        level.setMetadataJson(objectMapper.writeValueAsString(metadata));
                } catch (JsonProcessingException e) {
                        throw new ApiException("INTERNAL_ERROR", "Không thể cập nhật Level");
                }

                level.setUpdatedBy(educator.getId().toString());
                level = learningUnitRepository.save(level);

                LevelResponse response = LevelResponse.fromEntity(level);
                saveApprovalHistory(TYPE_LEVEL, level.getId(), educator, ContentStatus.APPROVED,
                                request.getComment() != null && !request.getComment().isBlank()
                                                ? request.getComment()
                                                : "Educator updated level (Auto-approved)",
                                response);
                return response;
        }

        @Transactional
        public void deleteLevel(String educatorEmail, UUID levelId) {
                getAccountByEmail(educatorEmail);

                LearningUnit level = learningUnitRepository.findById(levelId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));

                List<ContentItem> contentItems = contentItemRepository.findByLearningUnitId(levelId);
                if (!contentItems.isEmpty()) {
                        contentItemRepository.deleteAll(contentItems);
                }
                learningUnitRepository.delete(level);
        }

        @Transactional
        public void uploadLevelAudio(UUID levelId, String audioUrl) {
                LearningUnit level = learningUnitRepository.findById(levelId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));
                try {
                        Map<String, Object> metadata = level.getMetadataJson() != null
                                        ? new LinkedHashMap<>(objectMapper.readValue(level.getMetadataJson(),
                                                        new TypeReference<Map<String, Object>>() {
                                                        }))
                                        : new LinkedHashMap<>();
                        metadata.put("audio_url", audioUrl);
                        level.setMetadataJson(objectMapper.writeValueAsString(metadata));
                } catch (JsonProcessingException e) {
                        throw new ApiException("INTERNAL_ERROR", "Không thể cập nhật audio URL");
                }
                learningUnitRepository.save(level);
        }

        @Transactional(readOnly = true)
        public List<LevelResponse> getAllLevelsForSelection() {
                return learningUnitRepository.findByType(TYPE_LEVEL).stream()
                                .filter(unit -> {
                                        try {
                                                if (unit.getMetadataJson() == null)
                                                        return false;
                                                Map<String, Object> meta = objectMapper.readValue(
                                                                unit.getMetadataJson(),
                                                                new TypeReference<Map<String, Object>>() {
                                                                });
                                                return "APPROVED".equals(meta.get("status"));
                                        } catch (JsonProcessingException | ClassCastException e) {
                                                return false;
                                        }
                                })
                                .map(LevelResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        // ─── Challenge / ContentItem (PRONUNCIATION) ─────────────────────────────

        @Transactional
        public ChallengeResponse createChallenge(String educatorEmail, ChallengeCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
                LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));

                Map<String, Object> metadata = buildChallengeMetadata(request);
                metadata.put("status", STATUS_APPROVED);

                String title = request.getContentText() != null && request.getContentText().length() > 255
                                ? request.getContentText().substring(0, 252) + "..."
                                : request.getContentText();

                ContentItem challenge;
                try {
                        challenge = ContentItem.builder()
                                        .learningUnit(level)
                                        .title(title)
                                        .type(TYPE_PRONUNCIATION)
                                        .status(STATUS_APPROVED)
                                        .metadataJson(objectMapper.writeValueAsString(metadata))
                                        .build();
                } catch (JsonProcessingException e) {
                        throw new ApiException("INTERNAL_ERROR", "Không thể tạo Challenge");
                }
                challenge.setCreatedBy(educator.getId().toString());
                challenge = contentItemRepository.save(challenge);

                ChallengeResponse response = ChallengeResponse.fromEntity(challenge);
                saveApprovalHistory("CHALLENGE", challenge.getId(), educator, ContentStatus.APPROVED,
                                request.getComment() != null && !request.getComment().isBlank()
                                                ? request.getComment()
                                                : "Educator created challenge (Auto-approved)",
                                response);
                return response;
        }

        @Transactional
        public ChallengeResponse updateChallenge(String educatorEmail, UUID challengeId,
                        ChallengeCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
                ContentItem challenge = contentItemRepository.findById(challengeId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy Challenge"));

                LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));

                Map<String, Object> metadata = buildChallengeMetadata(request);

                String title = request.getContentText() != null && request.getContentText().length() > 255
                                ? request.getContentText().substring(0, 252) + "..."
                                : request.getContentText();

                try {
                        challenge.setLearningUnit(level);
                        challenge.setTitle(title);
                        challenge.setStatus(STATUS_APPROVED);
                        challenge.setMetadataJson(objectMapper.writeValueAsString(metadata));
                } catch (JsonProcessingException e) {
                        throw new ApiException("INTERNAL_ERROR", "Không thể cập nhật Challenge");
                }
                challenge.setUpdatedBy(educator.getId().toString());
                challenge = contentItemRepository.save(challenge);

                ChallengeResponse response = ChallengeResponse.fromEntity(challenge);
                saveApprovalHistory("CHALLENGE", challenge.getId(), educator, ContentStatus.APPROVED,
                                request.getComment() != null && !request.getComment().isBlank()
                                                ? request.getComment()
                                                : "Educator updated challenge (Auto-approved)",
                                response);
                return response;
        }

        @Transactional
        public void deleteChallenge(String educatorEmail, UUID id) {
                getAccountByEmail(educatorEmail);
                if (!contentItemRepository.existsById(id)) {
                        throw new ApiException(CODE_NOT_FOUND, "Không tìm thấy Challenge");
                }
                contentItemRepository.deleteById(id);
        }

        private Map<String, Object> buildChallengeMetadata(ChallengeCreateRequest request) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("content_text", request.getContentText());
                metadata.put("phonetic_transcription_ipa", request.getPhoneticTranscriptionIpa());
                metadata.put("reference_audio_url", request.getReferenceAudioUrl());
                metadata.put("focus_phonemes", request.getFocusPhonemes());
                metadata.put("skill_type", request.getSkillType());
                metadata.put("difficulty", request.getDifficulty() != null ? request.getDifficulty().name() : null);
                metadata.put("rejection_reason", "");
                return metadata;
        }

        // ─── Quiz / ContentItem (QUIZ) ────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<QuizResponse> getEducatorQuizzes(String educatorEmail) {
                Account educator = getAccountByEmail(educatorEmail);
                return contentItemRepository
                                .findByTypeAndCreatedByOrderByCreatedAtDesc("QUIZ", educator.getId().toString())
                                .stream()
                                .map(QuizResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public QuizResponse getQuizById(String educatorEmail, UUID quizId) {
                getAccountByEmail(educatorEmail);
                ContentItem quiz = contentItemRepository.findById(quizId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy b i kiểm tra"));
                return QuizResponse.fromEntity(quiz);
        }

        @Transactional
        public QuizResponse createQuiz(String educatorEmail, QuizCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
                LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));

                ContentItem quiz;
                try {
                        quiz = ContentItem.builder()
                                        .learningUnit(level)
                                        .title(request.getTitle())
                                        .type("QUIZ")
                                        .status(STATUS_APPROVED)
                                        .metadataJson(objectMapper.writeValueAsString(buildQuizMetadata(request)))
                                        .itemsJson(objectMapper
                                                        .writeValueAsString(buildQuestionsJson(request.getQuestions())))
                                        .build();
                } catch (JsonProcessingException e) {
                        throw new ApiException("INTERNAL_ERROR", "Không thể tạo Quiz");
                }
                quiz.setCreatedBy(educator.getId().toString());
                quiz = contentItemRepository.save(quiz);

                QuizResponse response = QuizResponse.fromEntity(quiz);
                saveApprovalHistory("QUIZ", quiz.getId(), educator, ContentStatus.APPROVED,
                                request.getComment() != null && !request.getComment().isBlank()
                                                ? request.getComment()
                                                : "Educator created quiz (Auto-approved)",
                                response);
                return response;
        }

        @Transactional
        public QuizResponse updateQuiz(String educatorEmail, UUID quizId, QuizCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
                ContentItem quiz = contentItemRepository.findById(quizId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy b i kiểm tra"));
                LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, MSG_LEVEL_NOT_FOUND));

                try {
                        quiz.setLearningUnit(level);
                        quiz.setTitle(request.getTitle());
                        quiz.setStatus(STATUS_APPROVED);
                        quiz.setMetadataJson(objectMapper.writeValueAsString(buildQuizMetadata(request)));
                        quiz.setItemsJson(objectMapper.writeValueAsString(buildQuestionsJson(request.getQuestions())));
                } catch (JsonProcessingException e) {
                        throw new ApiException("INTERNAL_ERROR", "Không thể cập nhật Quiz");
                }
                quiz.setUpdatedBy(educator.getId().toString());
                quiz = contentItemRepository.save(quiz);

                QuizResponse response = QuizResponse.fromEntity(quiz);
                saveApprovalHistory("QUIZ", quiz.getId(), educator, ContentStatus.APPROVED,
                                request.getComment() != null && !request.getComment().isBlank()
                                                ? request.getComment()
                                                : "Educator updated quiz (Auto-approved)",
                                response);
                return response;
        }

        private Map<String, Object> buildQuizMetadata(QuizCreateRequest request) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("description", request.getDescription());
                metadata.put("instructions", request.getInstructions());
                metadata.put("passing_score", request.getPassingScore());
                metadata.put("time_limit_seconds", request.getTimeLimitSeconds());
                metadata.put("rejection_reason", "");
                return metadata;
        }

        private List<Map<String, Object>> buildQuestionsJson(List<QuizQuestionRequest> questions) {
                if (questions == null)
                        return Collections.emptyList();
                return questions.stream()
                                .map(q -> {
                                        Map<String, Object> qMap = new LinkedHashMap<>();
                                        qMap.put("skill_type", q.getSkillType());
                                        qMap.put("difficulty", q.getDifficulty());
                                        qMap.put("question_order", q.getQuestionOrder());
                                        qMap.put("points", q.getPoints());
                                        qMap.put("challenge_id",
                                                        q.getChallengeId() != null ? q.getChallengeId().toString()
                                                                        : null);
                                        return qMap;
                                })
                                .collect(Collectors.toList());
        }

        // ─── Feedback ────────────────────────────────────────────────────────────

        @Transactional
        public void submitFeedback(String educatorEmail, UUID studentId, FeedbackCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
                SessionDetail sessionDetail = sessionDetailRepository.findById(request.getAttemptId())
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy lượt luyện tập"));

                EducatorFeedback feedback = EducatorFeedback.builder()
                                .educator(educator)
                                .sessionDetail(sessionDetail)
                                .comment(request.getComment())
                                .priority(request.getPriority())
                                .build();

                educatorFeedbackRepository.save(feedback);
        }

        // ─── Placement Rules ──────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<PlacementRuleResponse> getPlacementRules() {
                return placementRuleRepository.findAll().stream()
                                .map(PlacementRuleResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional
        public PlacementRuleResponse updateOrCreatePlacementRule(PlacementRuleRequest request) {
                LearningUnit errorTag = learningUnitRepository.findById(request.getErrorTagId())
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND,
                                                "Không tìm thấy ErrorTag cấu hình"));
                LearningUnit dialect = learningUnitRepository.findById(request.getDialectId())
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy Dialect cấu hình"));

                UUID errorTagId = request.getErrorTagId();
                UUID dialectId = request.getDialectId();

                PlacementRule rule = placementRuleRepository.findAll().stream()
                                .filter(r -> r.getErrorTag().getId().equals(errorTagId)
                                                && r.getTargetDialect().getId().equals(dialectId))
                                .findFirst()
                                .orElse(new PlacementRule());

                rule.setErrorTag(errorTag);
                rule.setThreshold(request.getThreshold());
                rule.setTargetDialect(dialect);
                rule.setCheckpoint(request.getCheckpoint());
                rule.setPriority(request.getPriority());

                return PlacementRuleResponse.fromEntity(placementRuleRepository.save(rule));
        }

        // ─── Dashboard ───────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public EducatorDashboardSummaryResponse getDashboardSummary(String educatorEmail) {
                getAccountByEmail(educatorEmail);

                long totalStudents = accountRepository.countByRoleCodeAndIsActiveTrue(
                                org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode.USER);
                long activeClassrooms = studentAssignmentRepository.countDistinctStudent();

                List<SessionDetail> details = sessionDetailRepository.findAll();
                long totalAttempts = details.size();
                double avgScore = details.stream()
                                .filter(d -> d.getScoreOverall() != null)
                                .mapToDouble(d -> d.getScoreOverall().doubleValue())
                                .average()
                                .orElse(0.0);

                return EducatorDashboardSummaryResponse.builder()
                                .activeClassrooms(activeClassrooms)
                                .totalStudents(totalStudents)
                                .totalAttempts(totalAttempts)
                                .averageClassScore(avgScore)
                                .build();
        }

        @Transactional(readOnly = true)
        public List<EducatorActivityResponse> getRecentActivities(String educatorEmail) {
                getAccountByEmail(educatorEmail);

                List<SessionDetail> recentDetails = sessionDetailRepository.findTop10ByOrderByCreatedAtDesc();

                return recentDetails.stream()
                                .map(sd -> {
                                        String studentName = sd.getSession().getAccount().getFullName();
                                        if (studentName == null || studentName.isEmpty()) {
                                                studentName = sd.getSession().getAccount().getEmail();
                                        }

                                        String unitName = sd.getContentItem() != null ? sd.getContentItem().getTitle()
                                                        : "b i học";
                                        double score = sd.getScoreOverall() != null ? sd.getScoreOverall().doubleValue()
                                                        : 0.0;

                                        return EducatorActivityResponse.builder()
                                                        .studentName(studentName)
                                                        .title(String.format(
                                                                        "Học viên %s đã ho n th nh %s với điểm số %.1f%%",
                                                                        studentName, unitName, score))
                                                        .createdAt(sd.getCreatedAt())
                                                        .type("COMPLETED")
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public StudentProfileResponse getStudentProfile(String educatorEmail, UUID studentId) {
                Account student = accountRepository.findById(studentId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy học viên"));

                AccountDashboardSummary summary = dashboardSummaryRepository.findByAccountId(studentId)
                                .orElse(AccountDashboardSummary.builder().build());

                List<StudentAssignment> assignments = studentAssignmentRepository.findByStudentIdAndStatus(studentId,
                                "PENDING");

                return StudentProfileResponse.builder()
                                .id(student.getId())
                                .email(student.getEmail())
                                .fullName(student.getFullName())
                                .totalXp(summary.getTotalXp())
                                .currentStreak(summary.getCurrentStreak())
                                .averageSpeakingScore(summary.getSpeakingScore())
                                .activeAssignments(assignments.stream()
                                                .map(a -> StudentProfileResponse.StudentAssignmentResponse.builder()
                                                                .id(a.getId())
                                                                .lessonTitle(a.getLearningUnit().getName())
                                                                .status(a.getStatus())
                                                                .deadline(a.getDeadline())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .build();
        }

        // ─── Student Analytics ───────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public StudentAnalyticsResponse getStudentAnalytics(String educatorEmail, UUID studentId) {
                Account student = accountRepository.findById(studentId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy học viên"));

                List<SessionDetail> details = sessionDetailRepository.findByAccountIdOrderByCreatedAtDesc(studentId);

                // 1. Top Errors (Phoneme level)
                List<PhonemeFeedbackDetail> feedbackList = details.stream()
                                .filter(sd -> sd.getAttemptMetadataJson() != null)
                                .flatMap(sd -> {
                                        try {
                                                Map<String, Object> meta = objectMapper.readValue(
                                                                sd.getAttemptMetadataJson(),
                                                                new TypeReference<Map<String, Object>>() {
                                                                });
                                                Object phonemeRaw = meta.get("phoneme_feedback_json");
                                                if (phonemeRaw == null)
                                                        return java.util.stream.Stream.empty();
                                                String phonemeJson = objectMapper.writeValueAsString(phonemeRaw);
                                                return objectMapper.readValue(phonemeJson,
                                                                new TypeReference<List<PhonemeFeedbackDetail>>() {
                                                                }).stream();
                                        } catch (JsonProcessingException | ClassCastException e) {
                                                log.error("Error deserializing phoneme feedback for session_detail {}",
                                                                sd.getId(), e);
                                                return Stream.empty();
                                        }
                                })
                                .collect(Collectors.toList());

                Map<String, List<PhonemeFeedbackDetail>> groupedByPhoneme = feedbackList.stream()
                                .collect(Collectors.groupingBy(PhonemeFeedbackDetail::getPhonemeIpa));

                List<StudentAnalyticsResponse.ErrorMetric> topErrors = groupedByPhoneme.entrySet().stream()
                                .map(entry -> {
                                        List<PhonemeFeedbackDetail> fb = entry.getValue();
                                        double avgScore = fb.stream()
                                                        .mapToDouble(f -> f.getScore().doubleValue())
                                                        .average()
                                                        .orElse(0.0);
                                        return new StudentAnalyticsResponse.ErrorMetric(entry.getKey(), avgScore,
                                                        fb.size());
                                })
                                .sorted(Comparator.comparing(StudentAnalyticsResponse.ErrorMetric::getAccuracy)
                                                .reversed())
                                .limit(5)
                                .collect(Collectors.toList());

                // 2. Score Trend (Daily history)
                Map<String, DoubleSummaryStatistics> dailyStats = details.stream()
                                .filter(d -> d.getScoreOverall() != null && d.getCreatedAt() != null)
                                .collect(Collectors.groupingBy(
                                                d -> d.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
                                                                .toLocalDate().toString(),
                                                Collectors.summarizingDouble(d -> d.getScoreOverall().doubleValue())));

                List<StudentAnalyticsResponse.DailyScore> scoreTrend = dailyStats.entrySet().stream()
                                .map(e -> new StudentAnalyticsResponse.DailyScore(e.getKey(),
                                                e.getValue().getAverage()))
                                .sorted(Comparator.comparing(StudentAnalyticsResponse.DailyScore::getDate))
                                .collect(Collectors.toList());

                return StudentAnalyticsResponse.builder()
                                .studentId(studentId)
                                .fullName(student.getFullName() != null ? student.getFullName() : "Học viên")
                                .topErrors(topErrors)
                                .scoreTrend(scoreTrend)
                                .build();
        }

        // ─── Approval History ────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<ContentApprovalHistoryResponse> getContentApprovalHistory(UUID contentId) {
                return contentApprovalHistoryRepository.findByContentIdOrderByCreatedAtDesc(contentId).stream()
                                .map(ContentApprovalHistoryResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        // ─── Helpers ─────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<EducatorFeedback> getStudentFeedbackHistory(String educatorEmail, UUID studentId) {
                return educatorFeedbackRepository.findBySessionDetailSessionAccountIdOrderByCreatedAtDesc(studentId);
        }

        private void saveApprovalHistory(String contentType, UUID contentId, Account educator,
                        ContentStatus status, String comment, Object responseDTO) {
                String contentSnapshot = "";
                try {
                        contentSnapshot = objectMapper.writeValueAsString(responseDTO);
                } catch (JsonProcessingException e) {
                        log.error("Failed to serialize {} content snapshot", contentType, e);
                }

                ContentApprovalHistory history = ContentApprovalHistory.builder()
                                .contentType(contentType)
                                .contentId(contentId)
                                .status(status)
                                .comment(comment)
                                .contentSnapshot(contentSnapshot)
                                .build();
                history.setCreatedBy(educator.getId().toString());
                contentApprovalHistoryRepository.save(history);
        }

        @Transactional(readOnly = true)
        public List<UserManagementResponse> getAssignedStudents(String educatorEmail) {
                // For simplicity, educators see all students in the regions they manage
                // This can be refined to manual assignments later
                return accountRepository
                                .findAllActiveByRoleCodeIn(List
                                                .of(org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode.USER))
                                .stream()
                                .map(UserManagementResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional
        public StudentAssignment assignLessonToStudent(String educatorEmail, UUID studentId, UUID unitId,
                        LocalDateTime deadline) {
                Account educator = getAccountByEmail(educatorEmail);
                Account student = accountRepository.findById(studentId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy học viên"));
                LearningUnit unit = learningUnitRepository.findById(unitId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy bài học"));

                StudentAssignment assignment = StudentAssignment.builder()
                                .educator(educator)
                                .student(student)
                                .learningUnit(unit)
                                .status("PENDING")
                                .deadline(deadline)
                                .build();

                return studentAssignmentRepository.save(assignment);
        }

        @Transactional
        public LessonPlan createLessonPlan(String educatorEmail, String title, String description, List<UUID> unitIds) {
                Account educator = getAccountByEmail(educatorEmail);

                LessonPlan plan = LessonPlan.builder()
                                .educator(educator)
                                .title(title)
                                .description(description)
                                .build();

                LessonPlan savedPlan = lessonPlanRepository.save(plan);

                List<LessonPlanItem> items = new ArrayList<>();
                for (int i = 0; i < unitIds.size(); i++) {
                        LearningUnit unit = learningUnitRepository.findById(unitIds.get(i))
                                        .orElseThrow(() -> new ApiException(CODE_NOT_FOUND,
                                                        "Không tìm thấy Unit ID: " + unitIds.get(i)));
                        items.add(LessonPlanItem.builder()
                                        .lessonPlan(savedPlan)
                                        .learningUnit(unit)
                                        .orderIndex(i)
                                        .build());
                }
                lessonPlanItemRepository.saveAll(items);
                savedPlan.setItems(items);
                return savedPlan;
        }

        @Transactional(readOnly = true)
        public List<LessonPlan> getLessonPlans(String educatorEmail) {
                Account educator = getAccountByEmail(educatorEmail);
                return lessonPlanRepository.findByEducatorId(educator.getId());
        }

        @Transactional
        public void assignPlanToStudent(String educatorEmail, UUID studentId, UUID planId, LocalDateTime deadline) {
                LessonPlan plan = lessonPlanRepository.findById(planId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy Lesson Plan"));

                for (LessonPlanItem item : plan.getItems()) {
                        assignLessonToStudent(educatorEmail, studentId, item.getLearningUnit().getId(), deadline);
                }
        }

        @Transactional
        public void updateStudentStatus(String educatorEmail, UUID studentId, boolean isActive) {
                Account student = accountRepository.findById(studentId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy học viên"));
                student.setIsActive(isActive);
                accountRepository.save(student);
        }

        @Transactional(readOnly = true)
        public List<LearnerDashboardResponse.DailyQuest> getStudentQuests(String educatorEmail, UUID studentId) {
                Account student = accountRepository.findById(studentId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy học viên"));

                java.time.LocalDate today = java.time.LocalDate.now();
                List<Quest> globalQuests = questRepository.findByIsActiveTrueAndStudentIdIsNull();
                List<Quest> personalQuests = questRepository.findByIsActiveTrueAndStudentId(studentId);
                List<Quest> allQuests = java.util.stream.Stream.concat(globalQuests.stream(), personalQuests.stream())
                                .filter(q -> q.getEndDate() == null || q.getEndDate().isAfter(LocalDateTime.now()))
                                .collect(Collectors.toList());

                List<AccountQuestProgress> userProgress = questProgressRepository.findByAccountIdAndQuestDate(studentId,
                                today);

                return allQuests.stream().map(quest -> {
                        Optional<AccountQuestProgress> progressOpt = userProgress.stream()
                                        .filter(p -> p.getQuest().getId().equals(quest.getId()))
                                        .findFirst();

                        int currentVal = progressOpt.map(AccountQuestProgress::getCurrentValue).orElse(0);
                        boolean isDone = progressOpt.map(AccountQuestProgress::getIsCompleted).orElse(false);
                        int progressPercent = (int) Math.min(100, (currentVal * 100.0) / quest.getTargetValue());

                        return LearnerDashboardResponse.DailyQuest.builder()
                                        .title(quest.getTitle())
                                        .xp("+" + quest.getXpReward() + " XP")
                                        .done(isDone)
                                        .progress(progressPercent)
                                        .build();
                }).collect(Collectors.toList());
        }

        @Transactional
        public Quest assignQuestToStudent(String educatorEmail, UUID studentId, EducatorQuestRequest request) {
                Account student = accountRepository.findById(studentId)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy học viên"));

                Quest quest = Quest.builder()
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .xpReward(request.getXpReward())
                                .targetValue(request.getTargetValue())
                                .questType(request.getQuestType())
                                .studentId(studentId)
                                .endDate(request.getEndDate())
                                .isActive(true)
                                .build();

                return questRepository.save(quest);
        }

        private Account getAccountByEmail(String email) {
                return accountRepository.findByEmail(email)
                                .orElseThrow(() -> new ApiException(CODE_NOT_FOUND, "Không tìm thấy t i khoản"));
        }

}
