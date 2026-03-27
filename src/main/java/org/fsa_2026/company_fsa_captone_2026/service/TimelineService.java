package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.response.timeline.CurrentProgressDto;
import org.fsa_2026.company_fsa_captone_2026.dto.response.timeline.TimelineItemDto;
import org.fsa_2026.company_fsa_captone_2026.dto.response.timeline.TimelineResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.StudySession;
import org.fsa_2026.company_fsa_captone_2026.exception.ResourceNotFoundException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineService {

    private final AccountRepository accountRepository;
    private final StudySessionRepository studySessionRepository;
    private final AccountLearningUnitRepository accountLearningUnitRepository;
    private final LearningUnitRepository learningUnitRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public TimelineResponse getLearningTimeline(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account", "id", accountId);
        }

        LocalDate today = LocalDate.now();
        Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<StudySession> pastSessions = studySessionRepository.findPastSessionsByAccountId(accountId, todayStart);
        List<AccountLearningUnit> progressRows = accountLearningUnitRepository.findByAccountIdWithLearningUnit(accountId);
        List<LearningUnit> allLevels = learningUnitRepository.findByType("LEVEL")
                .stream()
                .sorted(Comparator
                        .comparingInt(this::extractLevelOrder)
                        .thenComparing(LearningUnit::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        AccountLearningUnit current = findCurrentProgress(progressRows);
        Set<UUID> completedLevelIds = collectCompletedLevelIds(progressRows);

        List<TimelineItemDto> items = new ArrayList<>();
        items.addAll(buildPastItems(pastSessions));
        if (current != null) {
            items.add(buildPresentItem(today, current));
        }
        items.addAll(buildFutureItems(today, allLevels, completedLevelIds, current));

        return TimelineResponse.builder()
                .accountId(accountId)
                .todayDate(today)
                .currentProgress(toCurrentProgress(current))
                .timelineItems(items)
                .build();
    }

    private List<TimelineItemDto> buildPastItems(List<StudySession> sessions) {
        Map<LocalDate, PastAgg> byDate = new HashMap<>();

        for (StudySession session : sessions) {
            LocalDate date = LocalDate.ofInstant(session.getStartedAt(), ZoneId.systemDefault());
            PastAgg agg = byDate.computeIfAbsent(date, d -> new PastAgg());
            agg.sessionCount++;
            agg.xpEarned += extractXp(session.getSummaryJson());
        }

        return byDate.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, PastAgg>comparingByKey().reversed())
                .map(entry -> TimelineItemDto.builder()
                        .date(entry.getKey())
                        .status("completed")
                        .title("Đã hoàn thành " + entry.getValue().sessionCount + " phiên học")
                        .tooltipDetails("Tổng XP: " + entry.getValue().xpEarned + " | Số phiên: " + entry.getValue().sessionCount)
                        .locked(false)
                        .build())
                .toList();
    }

    private TimelineItemDto buildPresentItem(LocalDate today, AccountLearningUnit current) {
        LearningUnit level = current.getLearningUnit();
        int percent = calculateCompletionPercent(current);

        return TimelineItemDto.builder()
                .date(today)
                .status("in_progress")
                .title("Đang học: " + safe(level.getName()))
                .tooltipDetails("Tiến độ hiện tại: " + percent + "%")
                .locked(false)
                .build();
    }

    private List<TimelineItemDto> buildFutureItems(
            LocalDate today,
            List<LearningUnit> allLevels,
            Set<UUID> completedLevelIds,
            AccountLearningUnit current
    ) {
        UUID currentLevelId = current != null && current.getLearningUnit() != null
                ? current.getLearningUnit().getId()
                : null;

        List<LearningUnit> upcoming = allLevels.stream()
                .filter(level -> !completedLevelIds.contains(level.getId()))
                .filter(level -> currentLevelId == null || !level.getId().equals(currentLevelId))
                .limit(3)
                .toList();

        List<TimelineItemDto> result = new ArrayList<>();
        for (int i = 0; i < upcoming.size(); i++) {
            LearningUnit unit = upcoming.get(i);
            result.add(TimelineItemDto.builder()
                    .date(today.plusDays(i + 1L))
                    .status("upcoming")
                    .title("Sắp mở khóa: " + safe(unit.getName()))
                    .tooltipDetails("Bài học tiếp theo trong lộ trình")
                    .locked(true)
                    .build());
        }
        return result;
    }

    private CurrentProgressDto toCurrentProgress(AccountLearningUnit current) {
        if (current == null || current.getLearningUnit() == null) {
            return CurrentProgressDto.builder()
                    .mienDangHoc("Chưa xác định")
                    .chuongHienTai("Chưa có dữ liệu")
                    .phanTramHoanThanh(0)
                    .build();
        }

        LearningUnit level = current.getLearningUnit();
        String region = level.getParent() != null ? safe(level.getParent().getName()) : "Chua xac dinh";

        return CurrentProgressDto.builder()
                .mienDangHoc(region)
                .chuongHienTai(safe(level.getName()))
                .phanTramHoanThanh(calculateCompletionPercent(current))
                .build();
    }

    private AccountLearningUnit findCurrentProgress(List<AccountLearningUnit> rows) {
        return rows.stream()
                .sorted(Comparator
                        .comparingInt((AccountLearningUnit alu) -> extractLevelOrder(alu.getLearningUnit()))
                        .thenComparing(AccountLearningUnit::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(alu -> Boolean.FALSE.equals(alu.getIsCompleted()))
                .findFirst()
                .orElse(null);
    }

    private Set<UUID> collectCompletedLevelIds(List<AccountLearningUnit> rows) {
        Set<UUID> ids = new HashSet<>();
        for (AccountLearningUnit row : rows) {
            if (Boolean.TRUE.equals(row.getIsCompleted()) && row.getLearningUnit() != null) {
                ids.add(row.getLearningUnit().getId());
            }
        }
        return ids;
    }

    private int calculateCompletionPercent(AccountLearningUnit current) {
        if (Boolean.TRUE.equals(current.getIsCompleted())) {
            return 100;
        }
        if (current.getStarsEarned() != null && current.getStarsEarned() > 0) {
            return Math.min(99, (int) Math.round((current.getStarsEarned() / 3.0) * 100));
        }
        if (current.getHighestScore() != null) {
            return Math.min(99, current.getHighestScore().intValue());
        }
        return 0;
    }

    private int extractLevelOrder(LearningUnit unit) {
        if (unit == null || unit.getMetadataJson() == null || unit.getMetadataJson().isBlank()) {
            return Integer.MAX_VALUE;
        }
        try {
            Map<String, Object> meta = objectMapper.readValue(unit.getMetadataJson(), new TypeReference<>() {});
            Object val = meta.get("level_order");
            if (val instanceof Number number) {
                return number.intValue();
            }
            if (val instanceof String text && !text.isBlank()) {
                return Integer.parseInt(text);
            }
        } catch (Exception ex) {
            log.debug("Cannot parse level_order for unit {}", unit.getId(), ex);
        }
        return Integer.MAX_VALUE;
    }

    private int extractXp(String summaryJson) {
        if (summaryJson == null || summaryJson.isBlank()) {
            return 0;
        }
        try {
            Map<String, Object> summary = objectMapper.readValue(summaryJson, new TypeReference<>() {});
            Object xp = summary.get("xpEarned");
            if (xp instanceof Number number) {
                return number.intValue();
            }
            if (xp instanceof String text && !text.isBlank()) {
                return Integer.parseInt(text);
            }
        } catch (Exception ex) {
            log.debug("Cannot parse session summary xp", ex);
        }
        return 0;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private static class PastAgg {
        private int sessionCount;
        private int xpEarned;
    }
}
