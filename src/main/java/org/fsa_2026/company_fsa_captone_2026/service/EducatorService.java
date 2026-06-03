package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.UserManagementResponse;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ChatMessage;
import org.fsa_2026.company_fsa_captone_2026.entity.EducatorFeedback;
import org.fsa_2026.company_fsa_captone_2026.entity.CustomLearningPath;
import org.fsa_2026.company_fsa_captone_2026.entity.SessionDetail;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.MessageStatus;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChatMessageRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.CustomLearningPathRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.EducatorFeedbackRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.SessionDetailRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.SpeakingAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EducatorService {

    private static final String NOT_FOUND = "NOT_FOUND";

    private final AccountRepository accountRepository;
    private final SessionDetailRepository sessionDetailRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EducatorFeedbackRepository educatorFeedbackRepository;
    private final CustomLearningPathRepository customPathRepository;
    private final org.fsa_2026.company_fsa_captone_2026.repository.LessonPlanRepository lessonPlanRepository;
    private final SpeakingAttemptRepository speakingAttemptRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary(String educatorEmail) {
        resolveEducator(educatorEmail);
        List<Account> students = findStudents();
        List<SessionDetail> details = sessionDetailRepository.findAll();

        Double avg = speakingAttemptRepository.averageGroqScoreWithConsentGivenTrue();
        double avgPronunciationScore = avg != null ? avg : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalStudents", students.size());
        summary.put("activeStudents", students.stream().filter(a -> Boolean.TRUE.equals(a.getIsActive())).count());
        summary.put("averagePronunciationScore", Math.round(avgPronunciationScore * 10.0) / 10.0);
        summary.put("pendingFeedbackCount", educatorFeedbackRepository.count());
        summary.put("weeklyProgressRate", calculateWeeklyProgressRate(details));
        summary.put("pronunciationMetrics", buildPronunciationMetrics());
        
        List<Map<String, Object>> recentStudentData = students.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(Instant.now().minus(7, ChronoUnit.DAYS)))
                .sorted(Comparator.comparing(Account::getCreatedAt).reversed())
                .limit(5)
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", a.getId());
                    map.put("fullName", a.getFullName());
                    map.put("email", a.getEmail());
                    map.put("avatar", a.getAvatarUrl());
                    map.put("level", "A1");
                    return map;
                }).collect(Collectors.toList());
        summary.put("recentStudents", recentStudentData);
        
        // Cần truyền cả pendingFeedbacks thực sự thay vì empty array bên front-end
        List<Map<String, Object>> pendingFbs = educatorFeedbackRepository.findAll().stream()
                .sorted(Comparator.comparing(EducatorFeedback::getCreatedAt).reversed())
                .limit(3)
                .map(this::toFeedbackMap)
                .collect(Collectors.toList());
        summary.put("pendingFeedbacks", pendingFbs);

        return summary;
    }

    @Transactional(readOnly = true)
    public List<UserManagementResponse> getStudentAccounts(String educatorEmail) {
        List<Account> students = findStudents();
        List<CustomLearningPath> activePaths = customPathRepository.findByIsActiveTrue();

        // Map studentId -> isAiGenerated
        Map<UUID, Boolean> pathTypeMap = activePaths.stream()
                .collect(Collectors.toMap(
                        p -> p.getStudent().getId(),
                        p -> Boolean.TRUE.equals(p.getIsAiGenerated()),
                        (v1, v2) -> v1 // In case of duplicates, keep first (though isActive should be unique per
                                       // student)
                ));

        Account educator = resolveEducator(educatorEmail);
        List<Object[]> unreadCountsRaw = chatMessageRepository.countUnreadMessagesGroupedBySender(educator.getId());
        Map<UUID, Long> unreadMap = unreadCountsRaw.stream()
                .collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1], (v1, v2) -> v1));

        return students.stream()
                .map(student -> {
                    Boolean isAi = pathTypeMap.get(student.getId());
                    boolean hasPath = isAi != null;
                    String type = "NONE";
                    if (hasPath) {
                        type = isAi ? "AI" : "MANUAL";
                    }

                    UserManagementResponse resp = UserManagementResponse.fromEntity(student, hasPath, type);
                    resp.setUnreadCount(unreadMap.getOrDefault(student.getId(), 0L));
                    resp.setLastMessage("Mở hội thoại để xem...");
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(String educatorEmail, UUID studentId) {
        Account educator = resolveEducator(educatorEmail);
        chatMessageRepository.markMessagesAsRead(studentId, educator.getId());
    }

    @Transactional(readOnly = true)
    public UserManagementResponse getStudentAccountById(String educatorEmail, UUID studentId) {
        resolveEducator(educatorEmail);
        Account student = accountRepository.findById(studentId)
                .filter(account -> account.getRoleCode() == RoleCode.USER)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy học viên"));
        return UserManagementResponse.fromEntity(student);
    }

    @Transactional
    public Map<String, Object> createCustomLearningPath(String educatorEmail, UUID studentId, String title,
            String focusArea, List<String> milestones, String description) {
        Account educator = resolveEducator(educatorEmail);
        Account student = accountRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy học viên"));

        // Deactivate existing paths for this student
        List<CustomLearningPath> existingPaths = customPathRepository.findByStudentIdAndIsActiveTrue(studentId);
        existingPaths.forEach(p -> p.setIsActive(false));
        customPathRepository.saveAll(existingPaths);

        String serializedMilestones = "";
        try {
            serializedMilestones = objectMapper.writeValueAsString(milestones != null ? milestones : List.of());
        } catch (Exception ignored) {}

        CustomLearningPath path = CustomLearningPath.builder()
                .student(student)
                .educator(educator)
                .title(title)
                .description(description)
                .aiFeedback(serializedMilestones)
                .targetLevel(focusArea)
                .isAiGenerated(false)
                .isActive(true)
                .build();

        CustomLearningPath saved = customPathRepository.save(path);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("studentId", studentId);
        result.put("title", title);
        result.put("focusArea", focusArea);
        result.put("milestones", milestones != null ? milestones : List.of());
        result.put("description", description);
        result.put("status", "ACTIVE");
        result.put("updatedAt", saved.getCreatedAt() != null ? LocalDateTime.ofInstant(saved.getCreatedAt(), java.time.ZoneId.systemDefault()) : LocalDateTime.now());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProgressOverview(String educatorEmail) {
        resolveEducator(educatorEmail);
        List<Account> students = findStudents();
        List<SessionDetail> allDetails = sessionDetailRepository.findAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalStudents", students.size());
        response.put("activeStudents",
                students.stream().filter(a -> Boolean.TRUE.equals(a.getIsActive())).count());
        response.put("averagePronunciationScore", allDetails.stream()
                .filter(d -> d.getScoreOverall() != null)
                .mapToDouble(d -> d.getScoreOverall().doubleValue())
                .average().orElse(0.0));
        response.put("pendingFeedbackCount", educatorFeedbackRepository.count());
        response.put("weeklyProgressRate", calculateWeeklyProgressRate(allDetails));
        response.put("pronunciationMetrics", buildPronunciationMetrics());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPronunciationAnalytics(String educatorEmail, UUID studentId) {
        resolveEducator(educatorEmail);
        if (studentId != null) {
            accountRepository.findById(studentId)
                    .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy học viên"));
        }
        List<SessionDetail> details = studentId == null
                ? sessionDetailRepository.findAll()
                : sessionDetailRepository.findByAccountIdOrderByCreatedAtDesc(studentId);

        Map<String, Long> errorCounts = new LinkedHashMap<>();
        for (SessionDetail detail : details) {
            if (detail.getAttemptMetadataJson() == null)
                continue;
            try {
                Map<String, Object> meta = objectMapper.readValue(detail.getAttemptMetadataJson(),
                        new TypeReference<Map<String, Object>>() {
                        });
                Object phonemeRaw = meta.get("phoneme_feedback_json");
                if (phonemeRaw == null)
                    continue;
                String phonemeJson = objectMapper.writeValueAsString(phonemeRaw);
                List<Map<String, Object>> items = objectMapper.readValue(phonemeJson,
                        new TypeReference<List<Map<String, Object>>>() {
                        });
                for (Map<String, Object> item : items) {
                    String phoneme = String
                            .valueOf(item.getOrDefault("phonemeIpa", item.getOrDefault("phoneme", "unknown")));
                    errorCounts.put(phoneme, errorCounts.getOrDefault(phoneme, 0L) + 1);
                }
            } catch (JsonProcessingException e) {
                log.warn("Cannot parse phoneme feedback for session {}", detail.getId());
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("studentId", studentId);
        response.put("errors", errorCounts.entrySet().stream().map(e -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("phoneme", e.getKey());
            item.put("count", e.getValue());
            item.put("accuracy", Math.max(0, 100 - e.getValue() * 10));
            return item;
        }).toList());
        response.put("learningEffectiveness", List.of(
                Map.of("label", "Hoàn thành", "value", 72),
                Map.of("label", "Phát âm", "value", 64),
                Map.of("label", "Tự tin giao tiếp", "value", 58)));
        return response;
    }

    @Transactional
    public List<Map<String, Object>> getLessonPlans(String educatorEmail) {
        Account educator = resolveEducator(educatorEmail);
        List<org.fsa_2026.company_fsa_captone_2026.entity.LessonPlan> plans = lessonPlanRepository.findByEducatorId(educator.getId());
        
        if (plans.isEmpty()) {
            createLessonPlan(educatorEmail, "Luyện âm đầu L/N", "Cải thiện phân biệt phụ âm đầu",
                    List.of("Nguyễn Văn A", "Trần Thị B"), List.of("Đạt 80% chính xác", "Giảm lỗi đầu âm"));
            createLessonPlan(educatorEmail, "Nhịp điệu câu", "Tăng độ tự nhiên khi nói câu dài", 
                    List.of("Lê Văn C"), List.of("Đọc trôi chảy", "Giữ tốc độ ổn định"));
            plans = lessonPlanRepository.findByEducatorId(educator.getId());
        }

        return plans.stream().map(this::toLessonPlanMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createLessonPlan(String educatorEmail, String title, String objective,
            List<String> targetStudents, List<String> achievementGoals) {
        Account educator = resolveEducator(educatorEmail);
        
        String targetStudentsJson = "";
        String achievementGoalsJson = "";
        try {
            targetStudentsJson = objectMapper.writeValueAsString(targetStudents != null ? targetStudents : List.of());
            achievementGoalsJson = objectMapper.writeValueAsString(achievementGoals != null ? achievementGoals : List.of());
        } catch (Exception ignored) {}

        org.fsa_2026.company_fsa_captone_2026.entity.LessonPlan plan = org.fsa_2026.company_fsa_captone_2026.entity.LessonPlan.builder()
                .educator(educator)
                .title(title)
                .objective(objective)
                .targetStudentsJson(targetStudentsJson)
                .achievementGoalsJson(achievementGoalsJson)
                .status("PUBLISHED")
                .build();

        org.fsa_2026.company_fsa_captone_2026.entity.LessonPlan saved = lessonPlanRepository.save(plan);
        return toLessonPlanMap(saved);
    }

    @Transactional
    public Map<String, Object> updateLessonPlan(String educatorEmail, UUID id, String title, String objective,
            List<String> targetStudents, List<String> achievementGoals) {
        resolveEducator(educatorEmail);
        org.fsa_2026.company_fsa_captone_2026.entity.LessonPlan plan = lessonPlanRepository.findById(id)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy giáo án với ID: " + id));

        if (title != null && !title.isBlank()) {
            plan.setTitle(title);
        }
        if (objective != null && !objective.isBlank()) {
            plan.setObjective(objective);
        }
        try {
            if (targetStudents != null) {
                plan.setTargetStudentsJson(objectMapper.writeValueAsString(targetStudents));
            }
            if (achievementGoals != null) {
                plan.setAchievementGoalsJson(objectMapper.writeValueAsString(achievementGoals));
            }
        } catch (Exception ignored) {}

        org.fsa_2026.company_fsa_captone_2026.entity.LessonPlan saved = lessonPlanRepository.save(plan);
        return toLessonPlanMap(saved);
    }

    private Map<String, Object> toLessonPlanMap(org.fsa_2026.company_fsa_captone_2026.entity.LessonPlan plan) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", plan.getId().toString());
        map.put("title", plan.getTitle());
        map.put("objective", plan.getObjective());
        
        List<String> targetStudents = List.of();
        try {
            if (plan.getTargetStudentsJson() != null) {
                targetStudents = objectMapper.readValue(plan.getTargetStudentsJson(), new TypeReference<List<String>>() {});
            }
        } catch (Exception ignored) {}
        map.put("targetStudents", targetStudents);

        List<String> achievementGoals = List.of();
        try {
            if (plan.getAchievementGoalsJson() != null) {
                achievementGoals = objectMapper.readValue(plan.getAchievementGoalsJson(), new TypeReference<List<String>>() {});
            }
        } catch (Exception ignored) {}
        map.put("achievementGoals", achievementGoals);

        map.put("status", plan.getStatus());
        map.put("updatedAt", plan.getUpdatedAt() != null ? LocalDateTime.ofInstant(plan.getUpdatedAt(), java.time.ZoneId.systemDefault()) : LocalDateTime.now());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getConversationMessages(String educatorEmail, UUID studentId) {
        Account educator = resolveEducator(educatorEmail);
        return chatMessageRepository.findFullConversation(educator.getId(), studentId)
                .stream().map(this::toMessageMap)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> sendMessage(String educatorEmail, UUID studentId, String content) {
        Account educator = resolveEducator(educatorEmail);
        accountRepository.findById(studentId).orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy học viên"));
        ChatMessage message = ChatMessage.builder()
                .senderId(educator.getId())
                .recipientId(studentId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .status(MessageStatus.SENT)
                .build();
        return toMessageMap(chatMessageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFeedbackItems(String educatorEmail) {
        resolveEducator(educatorEmail);
        return educatorFeedbackRepository.findAll().stream().map(this::toFeedbackMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> sendFeedback(String educatorEmail, UUID studentId, String content, String priority) {
        Account educator = resolveEducator(educatorEmail);
        Account student = accountRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy học viên"));
        SessionDetail latest = sessionDetailRepository.findByAccountIdOrderByCreatedAtDesc(studentId).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy lượt luyện tập"));

        EducatorFeedback feedback = EducatorFeedback.builder()
                .educator(educator)
                .sessionDetail(latest)
                .comment(content)
                .priority(priority)
                .build();
        EducatorFeedback saved = educatorFeedbackRepository.save(feedback);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", saved.getId());
        item.put("studentId", student.getId());
        item.put("studentName", student.getFullName());
        item.put("content", content);
        item.put("channel", "FEEDBACK");
        item.put("priority", priority);
        item.put("createdAt", saved.getCreatedAt());
        return item;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAnalyticsReports(String educatorEmail) {
        resolveEducator(educatorEmail);
        return findStudents().stream().limit(5).map(student -> {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("studentId", student.getId());
            report.put("studentName", student.getFullName());
            report.put("pronunciationErrors", List.of(
                    Map.of("phoneme", "/l/", "count", 4, "accuracy", 72),
                    Map.of("phoneme", "/n/", "count", 2, "accuracy", 81)));
            report.put("learningEffectiveness", List.of(
                    Map.of("label", "Hoàn thành bài", "value", 72),
                    Map.of("label", "Ghi nhớ lỗi", "value", 64)));
            report.put("recentSessions", sessionDetailRepository.findByAccountIdOrderByCreatedAtDesc(student.getId())
                    .stream().limit(3).map(sd -> {
                        Map<String, Object> session = new LinkedHashMap<>();
                        session.put("id", sd.getId());
                        session.put("score", sd.getScoreOverall() != null ? sd.getScoreOverall().intValue() : 0);
                        session.put("createdAt", sd.getCreatedAt());
                        return session;
                    }).toList());
            return report;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalyticsReportByStudent(String educatorEmail, UUID studentId) {
        resolveEducator(educatorEmail);
        return getAnalyticsReports(educatorEmail).stream()
                .filter(r -> studentId.equals(r.get("studentId")))
                .findFirst()
                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy báo cáo"));
    }

    private Account resolveEducator(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    private List<Account> findStudents() {
        return accountRepository.findAllByRoleCodeIn(List.of(RoleCode.USER));
    }

    private List<Map<String, Object>> buildPronunciationMetrics() {
        List<Map<String, Object>> metrics = new ArrayList<>();
        
        long totalNorth = speakingAttemptRepository.countByDialectAndConsentGivenTrue("NORTH");
        long errNorth = speakingAttemptRepository.countByDialectAndConsentGivenTrueAndGroqScoreLessThan("NORTH", 80);
        int amDau = totalNorth > 0 ? (int) Math.round((1.0 - ((double) errNorth / totalNorth)) * 100) : 75;

        long totalSouth = speakingAttemptRepository.countByDialectAndConsentGivenTrue("SOUTH");
        long errSouth = speakingAttemptRepository.countByDialectAndConsentGivenTrueAndGroqScoreLessThan("SOUTH", 80);
        int nguyenAm = totalSouth > 0 ? (int) Math.round((1.0 - ((double) errSouth / totalSouth)) * 100) : 82;

        long totalCentral = speakingAttemptRepository.countByDialectAndConsentGivenTrue("CENTRAL");
        long errCentral = speakingAttemptRepository.countByDialectAndConsentGivenTrueAndGroqScoreLessThan("CENTRAL", 80);
        int nhipCau = totalCentral > 0 ? (int) Math.round((1.0 - ((double) errCentral / totalCentral)) * 100) : 88;
        
        metrics.add(metric("Âm đầu", amDau, amDau >= 70 ? "UP" : "STABLE"));
        metrics.add(metric("Nguyên âm", nguyenAm, nguyenAm >= 70 ? "UP" : "STABLE"));
        metrics.add(metric("Nhịp câu", nhipCau, nhipCau >= 70 ? "UP" : "STABLE"));
        return metrics;
    }

    private Map<String, Object> metric(String label, int value, String trend) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        item.put("trend", trend);
        return item;
    }

    private int percent(List<SessionDetail> details, int base) {
        return Math.min(100, base + details.size());
    }

    private double calculateWeeklyProgressRate(List<SessionDetail> details) {
        Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant twoWeeksAgo = Instant.now().minus(14, ChronoUnit.DAYS);
        
        long thisWeekCount = details.stream().filter(d -> d.getCreatedAt() != null && d.getCreatedAt().isAfter(oneWeekAgo)).count();
        long lastWeekCount = details.stream().filter(d -> d.getCreatedAt() != null && d.getCreatedAt().isAfter(twoWeeksAgo) && d.getCreatedAt().isBefore(oneWeekAgo)).count();
        
        if (lastWeekCount == 0 && thisWeekCount == 0) {
            long totalAttempts = speakingAttemptRepository.count();
            return totalAttempts > 0 ? 12.5 : 0.0;
        }
        
        if (lastWeekCount == 0) return thisWeekCount > 0 ? 100.0 : 0.0;
        double rate = ((double)(thisWeekCount - lastWeekCount) / lastWeekCount) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    private String defaultFocusArea(Account student) {
        return student.getRegion() != null ? student.getRegion() : "Pronunciation";
    }

    private Map<String, Object> lessonPlan(String id, String title, String objective, List<String> targetStudents,
            List<String> achievementGoals) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("id", id);
        plan.put("title", title);
        plan.put("objective", objective);
        plan.put("targetStudents", targetStudents != null ? targetStudents : List.of());
        plan.put("achievementGoals", achievementGoals != null ? achievementGoals : List.of());
        plan.put("status", "PUBLISHED");
        plan.put("updatedAt", LocalDateTime.now());
        return plan;
    }

    private Map<String, Object> toMessageMap(ChatMessage message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", message.getId());
        map.put("senderId", message.getSenderId());
        map.put("recipientId", message.getRecipientId());
        map.put("content", message.getContent());
        map.put("createdAt", message.getTimestamp());
        map.put("status", message.getStatus() != null ? message.getStatus().name() : "SENT");
        return map;
    }

    private Map<String, Object> toFeedbackMap(EducatorFeedback feedback) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", feedback.getId());
        Account student = null;
        if (feedback.getSessionDetail() != null && feedback.getSessionDetail().getSession() != null) {
            student = feedback.getSessionDetail().getSession().getAccount();
        }
        map.put("studentId", student != null ? student.getId() : null);
        map.put("studentName", student != null ? student.getFullName() : "Học viên");
        map.put("content", feedback.getComment());
        map.put("channel", "FEEDBACK");
        map.put("priority", feedback.getPriority());
        map.put("createdAt", feedback.getCreatedAt());
        return map;
    }
}
