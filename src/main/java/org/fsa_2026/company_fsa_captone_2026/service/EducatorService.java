package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ChatMessage;
import org.fsa_2026.company_fsa_captone_2026.entity.EducatorFeedback;
import org.fsa_2026.company_fsa_captone_2026.entity.SessionDetail;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.MessageStatus;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChatMessageRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.EducatorFeedbackRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.SessionDetailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary(String educatorEmail) {
        resolveEducator(educatorEmail);
        List<Account> students = findStudents();
        List<SessionDetail> details = sessionDetailRepository.findAll();

        double avgPronunciationScore = details.stream()
                .filter(d -> d.getScoreOverall() != null)
                .mapToDouble(d -> d.getScoreOverall().doubleValue())
                .average()
                .orElse(0.0);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalStudents", students.size());
        summary.put("activeStudents", students.stream().filter(a -> Boolean.TRUE.equals(a.getIsActive())).count());
        summary.put("averagePronunciationScore", avgPronunciationScore);
        summary.put("pendingFeedbackCount", educatorFeedbackRepository.count());
        summary.put("weeklyProgressRate", calculateWeeklyProgressRate(details));
        summary.put("pronunciationMetrics", buildPronunciationMetrics(details));
        return summary;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudentAccounts(String educatorEmail) {
        resolveEducator(educatorEmail);
        return findStudents().stream().map(student -> {
            Map<String, Object> learningPath = new LinkedHashMap<>();
            learningPath.put("id", UUID.nameUUIDFromBytes((student.getId() + ":path").getBytes()));
            learningPath.put("title", student.getFullName() != null ? student.getFullName() + " - Personalized Path" : "Personalized Path");
            learningPath.put("description", "Lộ trình học tùy chỉnh theo điểm phát âm và lỗi thường gặp");
            learningPath.put("focusArea", defaultFocusArea(student));
            learningPath.put("milestones", List.of("Ổn định nhịp phát âm", "Giảm lỗi nguyên âm", "Tăng độ chính xác"));
            learningPath.put("status", "ACTIVE");
            learningPath.put("updatedAt", LocalDateTime.now());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", student.getId());
            payload.put("fullName", student.getFullName());
            payload.put("email", student.getEmail());
            payload.put("avatar", student.getAvatarUrl());
            payload.put("level", student.getRegion() != null ? student.getRegion() : "N/A");
            payload.put("learningPath", learningPath);
            payload.put("lastActiveAt", student.getLastLoginDate() != null ? student.getLastLoginDate().atStartOfDay() : LocalDateTime.now().minusDays(1));
            payload.put("progressPercent", Math.min(100, student.getTotalExperience() / 10));
            payload.put("pronunciationScore", Math.min(100, 60 + (student.getTotalStars() == null ? 0 : student.getTotalStars())));
            payload.put("weakPhonemes", List.of("/l/", "/n/", "/tr/"));
            return payload;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentAccountById(String educatorEmail, UUID studentId) {
        resolveEducator(educatorEmail);
        Account student = accountRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy học viên"));
        return (Map<String, Object>) getStudentAccounts(educatorEmail).stream()
                .filter(item -> studentId.equals(item.get("id")))
                .findFirst()
                .orElseGet(() -> Map.of("id", student.getId(), "fullName", student.getFullName(), "email", student.getEmail()));
    }

    @Transactional
    public Map<String, Object> createCustomLearningPath(String educatorEmail, UUID studentId, String title, String focusArea, List<String> milestones, String description) {
        resolveEducator(educatorEmail);
        accountRepository.findById(studentId).orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy học viên"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", UUID.randomUUID());
        result.put("studentId", studentId);
        result.put("title", title);
        result.put("focusArea", focusArea);
        result.put("milestones", milestones != null ? milestones : List.of());
        result.put("description", description);
        result.put("status", "ACTIVE");
        result.put("updatedAt", LocalDateTime.now());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProgressOverview(String educatorEmail) {
        resolveEducator(educatorEmail);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalStudents", findStudents().size());
        response.put("activeStudents", findStudents().stream().filter(a -> Boolean.TRUE.equals(a.getIsActive())).count());
        response.put("averagePronunciationScore", sessionDetailRepository.findAll().stream()
                .filter(d -> d.getScoreOverall() != null)
                .mapToDouble(d -> d.getScoreOverall().doubleValue())
                .average().orElse(0.0));
        response.put("pendingFeedbackCount", educatorFeedbackRepository.count());
        response.put("weeklyProgressRate", calculateWeeklyProgressRate(sessionDetailRepository.findAll()));
        response.put("pronunciationMetrics", buildPronunciationMetrics(sessionDetailRepository.findAll()));
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPronunciationAnalytics(String educatorEmail, UUID studentId) {
        resolveEducator(educatorEmail);
        List<SessionDetail> details = studentId == null
                ? sessionDetailRepository.findAll()
                : sessionDetailRepository.findByAccountIdOrderByCreatedAtDesc(studentId);

        Map<String, Long> errorCounts = new LinkedHashMap<>();
        for (SessionDetail detail : details) {
            if (detail.getAttemptMetadataJson() == null) continue;
            try {
                Map<String, Object> meta = objectMapper.readValue(detail.getAttemptMetadataJson(), new TypeReference<Map<String, Object>>() {});
                Object phonemeRaw = meta.get("phoneme_feedback_json");
                if (phonemeRaw == null) continue;
                String phonemeJson = objectMapper.writeValueAsString(phonemeRaw);
                List<Map<String, Object>> items = objectMapper.readValue(phonemeJson, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> item : items) {
                    String phoneme = String.valueOf(item.getOrDefault("phonemeIpa", item.getOrDefault("phoneme", "unknown")));
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
                Map.of("label", "Tự tin giao tiếp", "value", 58)
        ));
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLessonPlans(String educatorEmail) {
        resolveEducator(educatorEmail);
        return List.of(
                lessonPlan("LP-001", "Luyện âm đầu L/N", "Cải thiện phân biệt phụ âm đầu", List.of("Nguyễn Văn A", "Trần Thị B"), List.of("Đạt 80% chính xác", "Giảm lỗi đầu âm")),
                lessonPlan("LP-002", "Nhịp điệu câu", "Tăng độ tự nhiên khi nói câu dài", List.of("Lê Văn C"), List.of("Đọc trôi chảy", "Giữ tốc độ ổn định"))
        );
    }

    @Transactional
    public Map<String, Object> createLessonPlan(String educatorEmail, String title, String objective, List<String> targetStudents, List<String> achievementGoals) {
        resolveEducator(educatorEmail);
        return lessonPlan(UUID.randomUUID().toString(), title, objective, targetStudents, achievementGoals);
    }

    @Transactional
    public Map<String, Object> updateLessonPlan(String educatorEmail, UUID id, String title, String objective, List<String> targetStudents, List<String> achievementGoals) {
        resolveEducator(educatorEmail);
        return lessonPlan(id.toString(), title, objective, targetStudents, achievementGoals);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMessages(String educatorEmail) {
        resolveEducator(educatorEmail);
        List<ChatMessage> messages = chatMessageRepository.findAll().stream()
                .sorted(Comparator.comparing(ChatMessage::getTimestamp).reversed())
                .limit(20)
                .collect(Collectors.toList());
        return messages.stream().map(this::toMessageMap).collect(Collectors.toList());
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
        Account student = accountRepository.findById(studentId).orElseThrow(() -> new ApiException(NOT_FOUND, "Không tìm thấy học viên"));
        SessionDetail latest = sessionDetailRepository.findByAccountIdOrderByCreatedAtDesc(studentId).stream().findFirst()
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
                    Map.of("phoneme", "/n/", "count", 2, "accuracy", 81)
            ));
            report.put("learningEffectiveness", List.of(
                    Map.of("label", "Hoàn thành bài", "value", 72),
                    Map.of("label", "Ghi nhớ lỗi", "value", 64)
            ));
            report.put("recentSessions", sessionDetailRepository.findByAccountIdOrderByCreatedAtDesc(student.getId()).stream().limit(3).map(sd -> {
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
        return accountRepository.findAll().stream()
                .filter(a -> a.getRoleCode() != null && "USER".equalsIgnoreCase(a.getRoleCode().name()))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildPronunciationMetrics(List<SessionDetail> details) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(metric("Âm đầu", percent(details, 65), "UP"));
        metrics.add(metric("Nguyên âm", percent(details, 58), "STABLE"));
        metrics.add(metric("Nhịp câu", percent(details, 71), "UP"));
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
        return Math.min(100.0, 40.0 + details.size() * 2.5);
    }

    private String defaultFocusArea(Account student) {
        return student.getRegion() != null ? student.getRegion() : "Pronunciation";
    }

    private Map<String, Object> lessonPlan(String id, String title, String objective, List<String> targetStudents, List<String> achievementGoals) {
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
        map.put("studentId", message.getRecipientId());
        Account student = accountRepository.findById(message.getRecipientId()).orElse(null);
        map.put("studentName", student != null ? student.getFullName() : "Học viên");
        map.put("content", message.getContent());
        map.put("channel", "MESSAGE");
        map.put("priority", "MEDIUM");
        map.put("createdAt", message.getTimestamp());
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
