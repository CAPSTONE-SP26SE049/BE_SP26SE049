package org.fsa_2026.company_fsa_captone_2026.service;

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

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class EducatorService {

        private final ClassroomRepository classroomRepository;
        private final ClassroomMemberRepository classroomMemberRepository;
        private final AccountRepository accountRepository;
        private final AttemptRepository attemptRepository;
        private final AttemptPhonemeFeedbackRepository phonemeFeedbackRepository;
        private final DialectRepository dialectRepository;
        private final LevelRepository levelRepository;
        private final EducatorFeedbackRepository educatorFeedbackRepository;
        private final PlacementRuleRepository placementRuleRepository;
        private final ChallengeRepository challengeRepository;
        private final ErrorTagRepository errorTagRepository;
        private final org.fsa_2026.company_fsa_captone_2026.repository.ContentApprovalHistoryRepository contentApprovalHistoryRepository;
        private final ObjectMapper objectMapper;

        @Transactional(readOnly = true)
        public ClassroomPerformanceResponse getClassroomPerformance(String educatorEmail, UUID classroomId) {
                Classroom classroom = classroomRepository.findById(classroomId)
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy lớp học"));
                validateEducatorOwnership(educatorEmail, classroom);

                List<Attempt> attempts = attemptRepository.findByClassroomId(classroomId);

                if (attempts.isEmpty()) {
                        return ClassroomPerformanceResponse.builder()
                                        .classroomId(classroomId.toString())
                                        .classroomName(classroom.getName())
                                        .averageScore(BigDecimal.ZERO)
                                        .completionRate(BigDecimal.ZERO)
                                        .commonErrors(Collections.emptyList())
                                        .build();
                }

                BigDecimal avgScore = BigDecimal.valueOf(attempts.stream()
                                .mapToDouble(a -> a.getScoreOverall().doubleValue())
                                .average()
                                .orElse(0.0));

                long passedCount = attempts.stream().filter(Attempt::getIsPassed).count();
                BigDecimal completionRate = BigDecimal.valueOf((double) passedCount / attempts.size() * 100);

                // Analyze common errors from phoneme feedback
                List<ClassroomPerformanceResponse.CommonErrorResponse> commonErrors = getCommonErrors(attempts);

                return ClassroomPerformanceResponse.builder()
                                .classroomId(classroomId.toString())
                                .classroomName(classroom.getName())
                                .averageScore(avgScore)
                                .completionRate(completionRate)
                                .commonErrors(commonErrors)
                                .build();
        }

        private List<ClassroomPerformanceResponse.CommonErrorResponse> getCommonErrors(List<Attempt> attempts) {
                List<UUID> attemptIds = attempts.stream().map(Attempt::getId).collect(Collectors.toList());
                List<AttemptPhonemeFeedback> allFeedback = phonemeFeedbackRepository.findByAttemptIdIn(attemptIds);

                Map<String, List<AttemptPhonemeFeedback>> groupedByPhoneme = allFeedback.stream()
                                .collect(Collectors.groupingBy(AttemptPhonemeFeedback::getPhonemeIpa));

                return groupedByPhoneme.entrySet().stream()
                                .map(entry -> {
                                        String phoneme = entry.getKey();
                                        List<AttemptPhonemeFeedback> feedbackList = entry.getValue();
                                        long count = feedbackList.size();
                                        double avgError = feedbackList.stream()
                                                        .mapToDouble(f -> f.getScore().doubleValue())
                                                        .average()
                                                        .orElse(0.0);

                                        return ClassroomPerformanceResponse.CommonErrorResponse.builder()
                                                        .phoneme(phoneme)
                                                        .occurrenceCount(count)
                                                        .averageErrorScore(BigDecimal.valueOf(avgError))
                                                        .build();
                                })
                                .sorted(Comparator.comparing(
                                                ClassroomPerformanceResponse.CommonErrorResponse::getAverageErrorScore))
                                .limit(5)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<LevelResponse> getCurriculumByRegion(String region) {
                Dialect dialect = dialectRepository.findByNameIgnoreCase(region)
                                .orElseThrow(() -> new ApiException("NOT_FOUND",
                                                "Không tìm thấy vùng miền: " + region));

                return levelRepository.findByDialectIdOrderByLevelOrderAsc(dialect.getId()).stream()
                                .map(LevelResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional
        public LevelResponse createLevel(String educatorEmail, LevelCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
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
                                .minStarsRequired(request.getMinStarsRequired() != null ? request.getMinStarsRequired()
                                                : 0)
                                .status(org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.PENDING)
                                .errorTag(errorTag)
                                .build();
                level = levelRepository.save(level);

                LevelResponse response = LevelResponse.fromEntity(level);
                saveApprovalHistory("LEVEL", level.getId(), educator, ContentStatus.PENDING,
                                request.getComment() != null && !request.getComment().isBlank()
                                                ? request.getComment()
                                                : "Educator created level",
                                response);

                return response;
        }

        @Transactional
        public LevelResponse updateLevel(String educatorEmail, UUID levelId, LevelUpdateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
                Level level = levelRepository.findById(levelId)
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy cấp độ"));

                Level targetLevel = level;
                boolean isNewDraft = false;

                if (org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.APPROVED
                                .equals(level.getStatus())) {
                        targetLevel = new Level();
                        targetLevel.setParent(level);
                        targetLevel.setStatus(org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.PENDING);

                        targetLevel.setDialect(level.getDialect());
                        targetLevel.setLevelOrder(level.getLevelOrder());
                        targetLevel.setName(level.getName());
                        targetLevel.setDescription(level.getDescription());
                        targetLevel.setMinStarsRequired(level.getMinStarsRequired());
                        targetLevel.setErrorTag(level.getErrorTag());
                        targetLevel.setAiThreshold(level.getAiThreshold());
                        targetLevel.setAudioUrl(level.getAudioUrl());
                        isNewDraft = true;
                } else {
                        targetLevel.setStatus(org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.PENDING);
                }

                targetLevel.setUpdatedBy(educator.getId().toString());

                if (request.getName() != null)
                        targetLevel.setName(request.getName());
                if (request.getAiThreshold() != null)
                        targetLevel.setAiThreshold(request.getAiThreshold());

                if (request.getErrorTagId() != null) {
                        org.fsa_2026.company_fsa_captone_2026.entity.ErrorTag errorTag = errorTagRepository
                                        .findById(request.getErrorTagId())
                                        .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Error Tag"));
                        targetLevel.setErrorTag(errorTag);
                }

                targetLevel = levelRepository.save(targetLevel);
                if (isNewDraft) {
                        level.setDraft(targetLevel);
                        levelRepository.save(level);
                }

                LevelResponse response = LevelResponse.fromEntity(targetLevel);
                saveApprovalHistory("LEVEL", level.getId(), educator, ContentStatus.PENDING,
                                request.getComment() != null && !request.getComment().isBlank()
                                                ? request.getComment()
                                                : "Educator updated level",
                                response);

                return response;
        }

        @Transactional
        public void deleteLevel(String educatorEmail, UUID levelId) {
                // Assert educator exists
                getAccountByEmail(educatorEmail);

                Level level = levelRepository.findById(levelId)
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy cấp độ"));

                // Cascade delete associated challenges to avoid orphan records
                List<Challenge> challenges = challengeRepository.findByLevelId(levelId);
                if (!challenges.isEmpty()) {
                        challengeRepository.deleteAll(challenges);
                }

                levelRepository.delete(level);
        }

        @Transactional
        public ChallengeResponse createChallenge(String educatorEmail, ChallengeCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
                Level level = levelRepository.findById(request.getLevelId())
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

                Challenge challenge = new Challenge();
                challenge.setLevel(level);
                challenge.setType(request.getType());
                challenge.setContentText(request.getContentText());
                challenge.setPhoneticTranscriptionIpa(request.getPhoneticTranscriptionIpa());
                challenge.setReferenceAudioUrl(request.getReferenceAudioUrl());
                challenge.setFocusPhonemes(request.getFocusPhonemes());
                challenge.setStatus(org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.PENDING);
                challenge.setCreatedBy(educator.getId().toString());

                challenge = challengeRepository.save(challenge);
                ChallengeResponse response = ChallengeResponse.fromEntity(challenge);

                saveApprovalHistory("CHALLENGE", challenge.getId(), educator, ContentStatus.PENDING,
                                request.getComment() != null && !request.getComment().isBlank()
                                                ? request.getComment()
                                                : "Educator created challenge",
                                response);

                return response;
        }

        @Transactional
        public ChallengeResponse updateChallenge(String educatorEmail, UUID challengeId,
                        ChallengeCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
                Challenge challenge = challengeRepository.findById(challengeId)
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Challenge"));

                Level level = levelRepository.findById(request.getLevelId())
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Level"));

                Challenge targetChallenge = challenge;
                boolean isNewDraft = false;

                if (org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.APPROVED
                                .equals(challenge.getStatus())) {
                        targetChallenge = new Challenge();
                        targetChallenge.setParent(challenge);
                        targetChallenge.setStatus(
                                        org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.PENDING);
                        isNewDraft = true;
                } else {
                        targetChallenge.setStatus(
                                        org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.PENDING);
                }

                targetChallenge.setLevel(level);
                targetChallenge.setType(request.getType());
                targetChallenge.setContentText(request.getContentText());
                targetChallenge.setPhoneticTranscriptionIpa(request.getPhoneticTranscriptionIpa());
                targetChallenge.setReferenceAudioUrl(request.getReferenceAudioUrl());
                targetChallenge.setFocusPhonemes(request.getFocusPhonemes());
                targetChallenge.setUpdatedBy(educator.getId().toString());

                targetChallenge = challengeRepository.save(targetChallenge);
                if (isNewDraft) {
                        challenge.setDraft(targetChallenge);
                        challengeRepository.save(challenge);
                }

                ChallengeResponse response = ChallengeResponse.fromEntity(targetChallenge);
                saveApprovalHistory("CHALLENGE", challenge.getId(), educator, ContentStatus.PENDING,
                                request.getComment() != null && !request.getComment().isBlank()
                                                ? request.getComment()
                                                : "Educator updated challenge",
                                response);

                return response;
        }

        @Transactional
        public void deleteChallenge(String educatorEmail, UUID id) {
                // Assert educator exists
                getAccountByEmail(educatorEmail);

                if (!challengeRepository.existsById(id)) {
                        throw new ApiException("NOT_FOUND", "Không tìm thấy Challenge");
                }
                challengeRepository.deleteById(id);
        }

        @Transactional
        public void uploadLevelAudio(UUID levelId, String audioUrl) {
                Level level = levelRepository.findById(levelId)
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy cấp độ"));

                level.setAudioUrl(audioUrl);
                levelRepository.save(level);
        }

        @Transactional
        public void submitFeedback(String educatorEmail, UUID studentId, FeedbackCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);
                Attempt attempt = attemptRepository.findById(request.getAttemptId())
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy lượt luyện tập"));

                EducatorFeedback feedback = EducatorFeedback.builder()
                                .educator(educator)
                                .attempt(attempt)
                                .comment(request.getComment())
                                .priority(request.getPriority())
                                .build();

                educatorFeedbackRepository.save(feedback);
        }

        @Transactional(readOnly = true)
        public List<PlacementRule> getPlacementRules() {
                return placementRuleRepository.findAll();
        }

        @Transactional
        public PlacementRule updateOrCreatePlacementRule(PlacementRuleRequest request) {
                // Fetch related entities
                ErrorTag errorTag = errorTagRepository.findById(request.getErrorTagId())
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy ErrorTag cấu hình"));
                Dialect dialect = dialectRepository.findById(request.getDialectId())
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect cấu hình"));

                List<PlacementRule> existing = placementRuleRepository.findAll();
                PlacementRule rule = existing.stream()
                                .filter(r -> r.getErrorTag().getId().equals(request.getErrorTagId())
                                                && r.getTargetDialect().getId().equals(request.getDialectId()))
                                .findFirst()
                                .orElse(new PlacementRule());

                rule.setErrorTag(errorTag);
                rule.setThreshold(request.getThreshold());
                rule.setTargetDialect(dialect);
                rule.setCheckpoint(request.getCheckpoint());
                rule.setPriority(request.getPriority());

                return placementRuleRepository.save(rule);
        }

        @Transactional(readOnly = true)
        public EducatorDashboardSummaryResponse getDashboardSummary(String educatorEmail) {
                Account educator = getAccountByEmail(educatorEmail);
                List<Classroom> classrooms = classroomRepository.findByEducatorId(educator.getId());

                long totalStudents = classrooms.stream()
                                .flatMap(c -> classroomMemberRepository.findByClassroomId(c.getId()).stream())
                                .map(m -> m.getStudent().getId())
                                .distinct()
                                .count();

                List<Attempt> classroomAttempts = classrooms.stream()
                                .flatMap(c -> attemptRepository.findByClassroomId(c.getId()).stream())
                                .collect(Collectors.toList());

                long totalAttempts = classroomAttempts.size();
                double avgScore = classroomAttempts.stream()
                                .mapToDouble(a -> a.getScoreOverall().doubleValue())
                                .average()
                                .orElse(0.0);

                return EducatorDashboardSummaryResponse.builder()
                                .activeClassrooms(classrooms.size())
                                .totalStudents(totalStudents)
                                .totalAttempts(totalAttempts)
                                .averageClassScore(avgScore)
                                .build();
        }

        @Transactional(readOnly = true)
        public List<ClassroomResponse> getClassrooms(String educatorEmail) {
                Account educator = getAccountByEmail(educatorEmail);
                return classroomRepository.findByEducatorId(educator.getId())
                                .stream()
                                .map(ClassroomResponse::fromEntity)
                                .collect(Collectors.toList());
        }

        @Transactional
        public ClassroomResponse createClassroom(String educatorEmail, ClassroomCreateRequest request) {
                Account educator = getAccountByEmail(educatorEmail);

                Classroom classroom = Classroom.builder()
                                .educator(educator)
                                .name(request.getName())
                                .code(generateClassCode())
                                .build();

                return ClassroomResponse.fromEntity(classroomRepository.save(classroom));
        }

        @Transactional
        public ClassroomResponse updateClassroom(String educatorEmail, UUID id, ClassroomCreateRequest request) {
                Classroom classroom = classroomRepository.findById(id)
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy lớp học"));

                validateEducatorOwnership(educatorEmail, classroom);

                if (request.getName() != null) {
                        classroom.setName(request.getName());
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
                                .orElseThrow(() -> new ApiException("NOT_FOUND",
                                                "Không tìm thấy học viên với email này"));

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

        @Transactional(readOnly = true)
        public StudentAnalyticsResponse getStudentAnalytics(String educatorEmail, UUID studentId) {
                Account student = accountRepository.findById(studentId)
                                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy học viên"));

                // Lấy tất cả feedback về phát âm của học viên này
                List<AttemptPhonemeFeedback> feedbackList = phonemeFeedbackRepository.findByStudentId(studentId);

                // Group by Phoneme IPA và tính điểm trung bình
                Map<String, List<AttemptPhonemeFeedback>> groupedByPhoneme = feedbackList.stream()
                                .collect(Collectors.groupingBy(AttemptPhonemeFeedback::getPhonemeIpa));

                List<StudentAnalyticsResponse.ErrorMetric> topErrors = groupedByPhoneme.entrySet().stream()
                                .map(entry -> {
                                        String phoneme = entry.getKey();
                                        List<AttemptPhonemeFeedback> feedback = entry.getValue();
                                        double avgScore = feedback.stream()
                                                        .mapToDouble(f -> f.getScore().doubleValue())
                                                        .average()
                                                        .orElse(0.0);
                                        return new StudentAnalyticsResponse.ErrorMetric(phoneme, avgScore,
                                                        feedback.size());
                                })
                                // Sắp xếp theo mức độ lỗi giảm dần (điểm càng cao càng lỗi nhiều)
                                .sorted(Comparator.comparing(StudentAnalyticsResponse.ErrorMetric::getAccuracy)
                                                .reversed())
                                .limit(5)
                                .collect(Collectors.toList());

                return StudentAnalyticsResponse.builder()
                                .studentId(studentId)
                                .fullName(student.getUserProfile() != null ? student.getUserProfile().getFullName()
                                                : "Học viên")
                                .topErrors(topErrors)
                                .build();
        }

        /**
         * Helper để lưu lịch sử phê duyệt nội dung
         */
        private void saveApprovalHistory(String contentType, UUID contentId, Account educator, ContentStatus status,
                        String comment, Object responseDTO) {
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

        @Transactional(readOnly = true)
        public List<ContentApprovalHistoryResponse> getContentApprovalHistory(UUID contentId) {
                return contentApprovalHistoryRepository.findByContentIdOrderByCreatedAtDesc(contentId).stream()
                                .map(ContentApprovalHistoryResponse::fromEntity)
                                .collect(Collectors.toList());
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
