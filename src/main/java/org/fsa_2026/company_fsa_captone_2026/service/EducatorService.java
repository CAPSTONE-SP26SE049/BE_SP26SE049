package org.fsa_2026.company_fsa_captone_2026.service;

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

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EducatorService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository classroomMemberRepository;
    private final AccountRepository accountRepository;
    private final SessionDetailRepository sessionDetailRepository;
    private final LearningUnitRepository learningUnitRepository;
    private final ContentItemRepository contentItemRepository;
    private final EducatorFeedbackRepository educatorFeedbackRepository;
    private final PlacementRuleRepository placementRuleRepository;
    private final ContentApprovalHistoryRepository contentApprovalHistoryRepository;
    private final ObjectMapper objectMapper;

    // ─── Classroom Performance ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ClassroomPerformanceResponse getClassroomPerformance(String educatorEmail, UUID classroomId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy lớp học"));
        validateEducatorOwnership(educatorEmail, classroom);

        List<SessionDetail> details = getSessionDetailsForClassroom(classroomId);

        if (details.isEmpty()) {
            return ClassroomPerformanceResponse.builder()
                    .classroomId(classroomId.toString())
                    .classroomName(classroom.getName())
                    .averageScore(BigDecimal.ZERO)
                    .completionRate(BigDecimal.ZERO)
                    .commonErrors(Collections.emptyList())
                    .build();
        }

        BigDecimal avgScore = BigDecimal.valueOf(details.stream()
                .filter(d -> d.getScoreOverall() != null)
                .mapToDouble(d -> d.getScoreOverall().doubleValue())
                .average()
                .orElse(0.0));

        long passedCount = details.stream().filter(d -> Boolean.TRUE.equals(d.getIsPassed())).count();
        BigDecimal completionRate = BigDecimal.valueOf((double) passedCount / details.size() * 100);

        return ClassroomPerformanceResponse.builder()
                .classroomId(classroomId.toString())
                .classroomName(classroom.getName())
                .averageScore(avgScore)
                .completionRate(completionRate)
                .commonErrors(getCommonErrors(details))
                .build();
    }

    private List<SessionDetail> getSessionDetailsForClassroom(UUID classroomId) {
        return classroomMemberRepository.findByClassroomId(classroomId).stream()
                .flatMap(m -> sessionDetailRepository
                        .findByAccountIdOrderByCreatedAtDesc(m.getStudent().getId()).stream())
                .collect(Collectors.toList());
    }

    private List<ClassroomPerformanceResponse.CommonErrorResponse> getCommonErrors(List<SessionDetail> details) {
        List<PhonemeFeedbackDetail> allFeedback = details.stream()
                .filter(sd -> sd.getAttemptMetadataJson() != null)
                .flatMap(sd -> {
                    try {
                        Map<String, Object> meta = objectMapper.readValue(
                                sd.getAttemptMetadataJson(), new TypeReference<Map<String, Object>>() {});
                        Object phonemeRaw = meta.get("phoneme_feedback_json");
                        if (phonemeRaw == null) return Stream.empty();
                        String phonemeJson = objectMapper.writeValueAsString(phonemeRaw);
                        return objectMapper.readValue(phonemeJson,
                                new TypeReference<List<PhonemeFeedbackDetail>>() {}).stream();
                    } catch (Exception e) {
                        log.error("Error deserializing phoneme feedback for session_detail {}", sd.getId(), e);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());

        Map<String, List<PhonemeFeedbackDetail>> groupedByPhoneme = allFeedback.stream()
                .collect(Collectors.groupingBy(PhonemeFeedbackDetail::getPhonemeIpa));

        return groupedByPhoneme.entrySet().stream()
                .map(entry -> {
                    List<PhonemeFeedbackDetail> feedbackList = entry.getValue();
                    double avgError = feedbackList.stream()
                            .mapToDouble(f -> f.getScore().doubleValue())
                            .average()
                            .orElse(0.0);
                    return ClassroomPerformanceResponse.CommonErrorResponse.builder()
                            .phoneme(entry.getKey())
                            .occurrenceCount((long) feedbackList.size())
                            .averageErrorScore(BigDecimal.valueOf(avgError))
                            .build();
                })
                .sorted(Comparator.comparing(r -> r.getAverageErrorScore()))
                .limit(5)
                .collect(Collectors.toList());
    }

    // ─── Curriculum / Level ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LevelResponse> getCurriculumByRegion(String region) {
        LearningUnit dialect = learningUnitRepository
                .findByTypeAndNameIgnoreCase("DIALECT", region)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy vùng miền: " + region));

        return learningUnitRepository
                .findByParentIdAndType(dialect.getId(), "LEVEL").stream()
                .map(LevelResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public LevelResponse createLevel(String educatorEmail, LevelCreateRequest request) {

        Account educator = getAccountByEmail(educatorEmail);

        LearningUnit parent = null;
        if (request.getParentId() != null) {
            parent = learningUnitRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy parent"));
        }

        LearningUnit level;
        try {
            level = LearningUnit.builder()
                    .parent(parent)
                    .name(request.getName())
                    .type(request.getType())
                    .metadataJson(objectMapper.writeValueAsString(
                            request.getMetadataJson() != null ? request.getMetadataJson() : new LinkedHashMap<>()))
                    .build();
        } catch (Exception e) {
            throw new ApiException("INTERNAL_ERROR", "Không thể tạo Learning Unit");
        }
        level.setCreatedBy(educator.getId().toString());
        level = learningUnitRepository.save(level);

        LevelResponse response = LevelResponse.fromEntity(level);
        saveApprovalHistory("LEVEL", level.getId(), educator, ContentStatus.PENDING,
                "Educator created level",
                response);
        return response;
    }

    @Transactional
    public LevelResponse updateLevel(String educatorEmail, UUID levelId, LevelCreateRequest request) {
        Account educator = getAccountByEmail(educatorEmail);
        LearningUnit level = learningUnitRepository.findById(levelId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy cấp độ"));

        if (request.getParentId() != null
                && (level.getParent() == null || !level.getParent().getId().equals(request.getParentId()))) {
            LearningUnit parent = learningUnitRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy parent"));
            level.setParent(parent);
        }

        level.setName(request.getName());
        level.setType(request.getType());

        try {
            Map<String, Object> metadata = request.getMetadataJson() != null
                    ? new LinkedHashMap<>(request.getMetadataJson())
                    : new LinkedHashMap<>();
            level.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            throw new ApiException("INTERNAL_ERROR", "Không thể cập nhật Level");
        }

        level.setUpdatedBy(educator.getId().toString());
        level = learningUnitRepository.save(level);

        LevelResponse response = LevelResponse.fromEntity(level);
        saveApprovalHistory("LEVEL", level.getId(), educator, ContentStatus.PENDING,
                "Educator updated level",
                response);
        return response;
    }

    @Transactional
    public void deleteLevel(String educatorEmail, UUID levelId) {
        getAccountByEmail(educatorEmail);

        LearningUnit level = learningUnitRepository.findById(levelId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy cấp độ"));

        List<ContentItem> contentItems = contentItemRepository.findByLearningUnitId(levelId);
        if (!contentItems.isEmpty()) {
            contentItemRepository.deleteAll(contentItems);
        }
        learningUnitRepository.delete(level);
    }

    @Transactional
    public void uploadLevelAudio(UUID levelId, String audioUrl) {
        LearningUnit level = learningUnitRepository.findById(levelId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy cấp độ"));
        try {
            Map<String, Object> metadata = level.getMetadataJson() != null
                    ? new LinkedHashMap<>(objectMapper.readValue(level.getMetadataJson(), new TypeReference<Map<String, Object>>() {}))
                    : new LinkedHashMap<>();
            metadata.put("audio_url", audioUrl);
            level.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            throw new ApiException("INTERNAL_ERROR", "Không thể cập nhật audio URL");
        }
        learningUnitRepository.save(level);
    }

    @Transactional(readOnly = true)
    public List<LevelResponse> getAllLevelsForSelection() {
        return learningUnitRepository.findByType("LEVEL").stream()
                .filter(unit -> {
                    try {
                        if (unit.getMetadataJson() == null) return false;
                        Map<String, Object> meta = objectMapper.readValue(
                                unit.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
                        return "APPROVED".equals(meta.get("status"));
                    } catch (Exception e) {
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
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        Map<String, Object> metadata = buildChallengeMetadata(request);

        String title = request.getContentText() != null && request.getContentText().length() > 255
                ? request.getContentText().substring(0, 252) + "..."
                : request.getContentText();

        ContentItem challenge;
        try {
            challenge = ContentItem.builder()
                    .learningUnit(level)
                    .title(title)
                    .type("PRONUNCIATION")
                    .status("PENDING")
                    .metadataJson(objectMapper.writeValueAsString(metadata))
                    .build();
        } catch (Exception e) {
            throw new ApiException("INTERNAL_ERROR", "Không thể tạo Challenge");
        }
        challenge.setCreatedBy(educator.getId().toString());
        challenge = contentItemRepository.save(challenge);

        ChallengeResponse response = ChallengeResponse.fromEntity(challenge);
        saveApprovalHistory("CHALLENGE", challenge.getId(), educator, ContentStatus.PENDING,
                request.getComment() != null && !request.getComment().isBlank()
                        ? request.getComment() : "Educator created challenge",
                response);
        return response;
    }

    @Transactional
    public ChallengeResponse updateChallenge(String educatorEmail, UUID challengeId,
            ChallengeCreateRequest request) {
        Account educator = getAccountByEmail(educatorEmail);
        ContentItem challenge = contentItemRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Challenge"));

        LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        Map<String, Object> metadata = buildChallengeMetadata(request);

        String title = request.getContentText() != null && request.getContentText().length() > 255
                ? request.getContentText().substring(0, 252) + "..."
                : request.getContentText();

        try {
            challenge.setLearningUnit(level);
            challenge.setTitle(title);
            challenge.setStatus("PENDING");
            challenge.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            throw new ApiException("INTERNAL_ERROR", "Không thể cập nhật Challenge");
        }
        challenge.setUpdatedBy(educator.getId().toString());
        challenge = contentItemRepository.save(challenge);

        ChallengeResponse response = ChallengeResponse.fromEntity(challenge);
        saveApprovalHistory("CHALLENGE", challenge.getId(), educator, ContentStatus.PENDING,
                request.getComment() != null && !request.getComment().isBlank()
                        ? request.getComment() : "Educator updated challenge",
                response);
        return response;
    }

    @Transactional
    public void deleteChallenge(String educatorEmail, UUID id) {
        getAccountByEmail(educatorEmail);
        if (!contentItemRepository.existsById(id)) {
            throw new ApiException("NOT_FOUND", "Không tìm thấy Challenge");
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
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy bài kiểm tra"));
        return QuizResponse.fromEntity(quiz);
    }

    @Transactional
    public QuizResponse createQuiz(String educatorEmail, QuizCreateRequest request) {
        Account educator = getAccountByEmail(educatorEmail);
        LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        ContentItem quiz;
        try {
            quiz = ContentItem.builder()
                    .learningUnit(level)
                    .title(request.getTitle())
                    .type("QUIZ")
                    .status("PENDING")
                    .metadataJson(objectMapper.writeValueAsString(buildQuizMetadata(request)))
                    .itemsJson(objectMapper.writeValueAsString(buildQuestionsJson(request.getQuestions())))
                    .build();
        } catch (Exception e) {
            throw new ApiException("INTERNAL_ERROR", "Không thể tạo Quiz");
        }
        quiz.setCreatedBy(educator.getId().toString());
        quiz = contentItemRepository.save(quiz);

        QuizResponse response = QuizResponse.fromEntity(quiz);
        saveApprovalHistory("QUIZ", quiz.getId(), educator, ContentStatus.PENDING,
                request.getComment() != null && !request.getComment().isBlank()
                        ? request.getComment() : "Educator created quiz",
                response);
        return response;
    }

    @Transactional
    public QuizResponse updateQuiz(String educatorEmail, UUID quizId, QuizCreateRequest request) {
        Account educator = getAccountByEmail(educatorEmail);
        ContentItem quiz = contentItemRepository.findById(quizId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy bài kiểm tra"));
        LearningUnit level = learningUnitRepository.findById(request.getLevelId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

        try {
            quiz.setLearningUnit(level);
            quiz.setTitle(request.getTitle());
            quiz.setStatus("PENDING");
            quiz.setMetadataJson(objectMapper.writeValueAsString(buildQuizMetadata(request)));
            quiz.setItemsJson(objectMapper.writeValueAsString(buildQuestionsJson(request.getQuestions())));
        } catch (Exception e) {
            throw new ApiException("INTERNAL_ERROR", "Không thể cập nhật Quiz");
        }
        quiz.setUpdatedBy(educator.getId().toString());
        quiz = contentItemRepository.save(quiz);

        QuizResponse response = QuizResponse.fromEntity(quiz);
        saveApprovalHistory("QUIZ", quiz.getId(), educator, ContentStatus.PENDING,
                request.getComment() != null && !request.getComment().isBlank()
                        ? request.getComment() : "Educator updated quiz",
                response);
        return response;
    }

    private Map<String, Object> buildQuizMetadata(QuizCreateRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("description", request.getDescription());
        metadata.put("instructions", request.getInstructions());
        metadata.put("passing_score", request.getPassingScore());
        metadata.put("time_limit_minutes", request.getTimeLimitMinutes());
        metadata.put("rejection_reason", "");
        return metadata;
    }

    private List<Map<String, Object>> buildQuestionsJson(List<QuizQuestionRequest> questions) {
        if (questions == null) return Collections.emptyList();
        return questions.stream()
                .map(q -> {
                    Map<String, Object> qMap = new LinkedHashMap<>();
                    qMap.put("skill_type", q.getSkillType());
                    qMap.put("difficulty", q.getDifficulty());
                    qMap.put("question_order", q.getQuestionOrder());
                    qMap.put("points", q.getPoints());
                    qMap.put("challenge_id", q.getChallengeId() != null ? q.getChallengeId().toString() : null);
                    return qMap;
                })
                .collect(Collectors.toList());
    }

    // ─── Feedback ────────────────────────────────────────────────────────────

    @Transactional
    public void submitFeedback(String educatorEmail, UUID studentId, FeedbackCreateRequest request) {
        Account educator = getAccountByEmail(educatorEmail);
        SessionDetail sessionDetail = sessionDetailRepository.findById(request.getAttemptId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy lượt luyện tập"));

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
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy ErrorTag cấu hình"));
        LearningUnit dialect = learningUnitRepository.findById(request.getDialectId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect cấu hình"));

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
        Account educator = getAccountByEmail(educatorEmail);
        List<Classroom> classrooms = classroomRepository.findByEducatorId(educator.getId());

        long totalStudents = classrooms.stream()
                .flatMap(c -> classroomMemberRepository.findByClassroomId(c.getId()).stream())
                .map(m -> m.getStudent().getId())
                .distinct()
                .count();

        List<SessionDetail> classroomDetails = classrooms.stream()
                .flatMap(c -> getSessionDetailsForClassroom(c.getId()).stream())
                .collect(Collectors.toList());

        long totalAttempts = classroomDetails.size();
        double avgScore = classroomDetails.stream()
                .filter(d -> d.getScoreOverall() != null)
                .mapToDouble(d -> d.getScoreOverall().doubleValue())
                .average()
                .orElse(0.0);

        return EducatorDashboardSummaryResponse.builder()
                .activeClassrooms(classrooms.size())
                .totalStudents(totalStudents)
                .totalAttempts(totalAttempts)
                .averageClassScore(avgScore)
                .build();
    }

    // ─── Student Analytics ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StudentAnalyticsResponse getStudentAnalytics(String educatorEmail, UUID studentId) {
        Account student = accountRepository.findById(studentId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy học viên"));

        List<SessionDetail> details = sessionDetailRepository.findByAccountIdOrderByCreatedAtDesc(studentId);

        List<PhonemeFeedbackDetail> feedbackList = details.stream()
                .filter(sd -> sd.getAttemptMetadataJson() != null)
                .flatMap(sd -> {
                    try {
                        Map<String, Object> meta = objectMapper.readValue(
                                sd.getAttemptMetadataJson(), new TypeReference<Map<String, Object>>() {});
                        Object phonemeRaw = meta.get("phoneme_feedback_json");
                        if (phonemeRaw == null) return Stream.empty();
                        String phonemeJson = objectMapper.writeValueAsString(phonemeRaw);
                        return objectMapper.readValue(phonemeJson,
                                new TypeReference<List<PhonemeFeedbackDetail>>() {}).stream();
                    } catch (Exception e) {
                        log.error("Error deserializing phoneme feedback for session_detail {}", sd.getId(), e);
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
                    return new StudentAnalyticsResponse.ErrorMetric(entry.getKey(), avgScore, fb.size());
                })
                .sorted(Comparator.comparing(StudentAnalyticsResponse.ErrorMetric::getAccuracy).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return StudentAnalyticsResponse.builder()
                .studentId(studentId)
                .fullName(student.getFullName() != null ? student.getFullName() : "Học viên")
                .topErrors(topErrors)
                .build();
    }

    // ─── Classrooms ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ClassroomResponse> getClassrooms(String educatorEmail) {
        Account educator = getAccountByEmail(educatorEmail);
        return classroomRepository.findByEducatorId(educator.getId()).stream()
                .map(ClassroomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClassroomResponse createClassroom(String educatorEmail, ClassroomCreateRequest request) {
        Account educator = getAccountByEmail(educatorEmail);

        LearningUnit dialect = null;
        if (request.getDialectId() != null) {
            dialect = learningUnitRepository.findById(request.getDialectId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy vùng phương ngữ"));
        }

        Classroom classroom = Classroom.builder()
                .educator(educator)
                .name(request.getName())
                .code(generateClassCode())
                .description(request.getDescription())
                .dialect(dialect)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .currentStudents(request.getCurrentStudents() != null ? request.getCurrentStudents() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        return ClassroomResponse.fromEntity(classroomRepository.save(classroom));
    }

    @Transactional
    public ClassroomResponse updateClassroom(String educatorEmail, UUID id, ClassroomCreateRequest request) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy lớp học"));
        validateEducatorOwnership(educatorEmail, classroom);

        if (request.getName() != null) classroom.setName(request.getName());
        if (request.getDescription() != null) classroom.setDescription(request.getDescription());
        if (request.getStartDate() != null) classroom.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) classroom.setEndDate(request.getEndDate());
        if (request.getCurrentStudents() != null) classroom.setCurrentStudents(request.getCurrentStudents());
        if (request.getIsActive() != null) classroom.setIsActive(request.getIsActive());
        if (request.getDialectId() != null) {
            LearningUnit dialect = learningUnitRepository.findById(request.getDialectId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy vùng phương ngữ"));
            classroom.setDialect(dialect);
        }

        return ClassroomResponse.fromEntity(classroomRepository.save(classroom));
    }

    @Transactional
    public void deleteClassroom(String educatorEmail, UUID id) {
        Classroom classroom = classroomRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy lớp học"));
        validateEducatorOwnership(educatorEmail, classroom);
        classroomRepository.delete(classroom);
    }

    @Transactional(readOnly = true)
    public List<UserManagementResponse> getClassroomStudents(String educatorEmail, UUID classroomId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy lớp học"));
        validateEducatorOwnership(educatorEmail, classroom);
        return classroomMemberRepository.findByClassroomId(classroomId).stream()
                .map(member -> UserManagementResponse.fromEntity(member.getStudent()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addStudentToClassroom(String educatorEmail, UUID classroomId, AddStudentRequest request) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy lớp học"));
        validateEducatorOwnership(educatorEmail, classroom);

        Account student = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy học viên với email này"));

        if (classroomMemberRepository.existsByClassroomIdAndStudentId(classroomId, student.getId())) {
            throw new ApiException("BAD_REQUEST", "Học viên này đã có trong lớp");
        }

        ClassroomMember member = ClassroomMember.builder()
                .classroom(classroom)
                .student(student)
                .build();
        classroomMemberRepository.save(member);
    }

    @Transactional
    public void removeStudentFromClassroom(String educatorEmail, UUID classroomId, UUID studentId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy lớp học"));
        validateEducatorOwnership(educatorEmail, classroom);
        classroomMemberRepository.deleteByClassroomIdAndStudentId(classroomId, studentId);
    }

    // ─── Approval History ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ContentApprovalHistoryResponse> getContentApprovalHistory(UUID contentId) {
        return contentApprovalHistoryRepository.findByContentIdOrderByCreatedAtDesc(contentId).stream()
                .map(ContentApprovalHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void saveApprovalHistory(String contentType, UUID contentId, Account educator,
            ContentStatus status, String comment, Object responseDTO) {
        String contentSnapshot = "";
        try {
            contentSnapshot = objectMapper.writeValueAsString(responseDTO);
        } catch (Exception e) {
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

    private Account getAccountByEmail(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy tài khoản"));
    }

    private void validateEducatorOwnership(String educatorEmail, Classroom classroom) {
        if (!classroom.getEducator().getEmail().equals(educatorEmail)) {
            throw new ApiException("FORBIDDEN", "Bạn không có quyền quản lý lớp học này");
        }
    }

    private String generateClassCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
