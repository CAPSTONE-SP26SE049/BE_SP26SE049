package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.StudySession;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyChallengeService {

    private final ChallengeBankRepository challengeBankRepository;
    private final SystemConfigService systemConfigService;
    private final StudySessionRepository studySessionRepository;
    private final AccountRepository accountRepository;
    private final AIService aiService;
    private final FirebaseStorageService firebaseStorageService;
    private final ObjectMapper objectMapper;
    private final BadgeUnlockService badgeUnlockService;

    /**
     * Lấy 3 câu hỏi thử thách của hôm nay. Tự động xoay tua nếu sang ngày mới.
     */
    @Transactional
    public List<ChallengeBank> getDailyChallenges() {
        String todayStr = LocalDate.now().toString();
        String configDate = systemConfigService.getValue("daily.challenge.date", "");
        String configIds = systemConfigService.getValue("daily.challenge.ids", "");

        List<ChallengeBank> challenges = new ArrayList<>();
        if (todayStr.equals(configDate) && !configIds.isBlank()) {
            String[] ids = configIds.split(",");
            for (String idStr : ids) {
                try {
                    UUID uuid = UUID.fromString(idStr.trim());
                    challengeBankRepository.findById(uuid).ifPresent(challenges::add);
                } catch (Exception e) {
                    log.warn("Invalid daily challenge ID format in system config: {}", idStr);
                }
            }
        }

        // Nếu không có câu nào hoặc ngày bị lệch, thực hiện xoay tua mới
        if (challenges.isEmpty()) {
            List<ChallengeBank> all = challengeBankRepository.findAll();
            if (all.isEmpty()) {
                log.warn("ChallengeBank is empty. Cannot rotate daily challenges.");
                return Collections.emptyList();
            }
            List<ChallengeBank> selected;
            if (all.size() <= 3) {
                selected = all;
            } else {
                List<ChallengeBank> shuffled = new ArrayList<>(all);
                Collections.shuffle(shuffled);
                selected = shuffled.subList(0, 3);
            }

            String newIds = selected.stream()
                    .map(c -> c.getId().toString())
                    .collect(Collectors.joining(","));

            Map<String, String> configs = new HashMap<>();
            configs.put("daily.challenge.date", todayStr);
            configs.put("daily.challenge.ids", newIds);
            systemConfigService.updateConfigs(configs);

            challenges = selected;
        }

        return challenges;
    }

    /**
     * Nộp ghi âm bài làm của 1 trong 3 câu thử thách, chấm điểm và trao thưởng.
     */
    @Transactional
    public Map<String, Object> submitDailyChallenge(String email, UUID challengeId, MultipartFile audio, String dialect) throws IOException {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy tài khoản"));

        ChallengeBank challenge = challengeBankRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy thử thách"));

        // 1. Đánh giá phát âm qua AI
        String focusErrorTag = null;
        if (dialect != null && !dialect.isBlank()) {
            focusErrorTag = aiService.findErrorTagUnitId(dialect);
        }
        Map<String, Object> evaluation = aiService.evaluatePronunciation(audio.getBytes(), challenge.getContentText(), focusErrorTag);

        // 2. Upload file âm thanh lên Firebase
        String audioUrl = "";
        try {
            audioUrl = firebaseStorageService.uploadFile(audio, "daily-challenge-attempts");
        } catch (Exception e) {
            log.error("Failed to upload daily challenge audio to Firebase", e);
        }

        // 3. Tạo và lưu StudySession loại DAILY
        boolean isCorrect = Boolean.TRUE.equals(evaluation.get("isCorrect")) || 
                ((Number) evaluation.getOrDefault("accuracy", 0)).intValue() >= 80;

        Map<String, Object> summaryMap = new HashMap<>();
        summaryMap.put("challengeId", challengeId.toString());
        summaryMap.put("isCorrect", isCorrect);
        summaryMap.put("score", evaluation.getOrDefault("accuracy", 0));
        summaryMap.put("audioUrl", audioUrl);
        summaryMap.put("feedback", evaluation.getOrDefault("feedback", ""));
        summaryMap.put("targetText", challenge.getContentText());

        String summaryJson = "";
        try {
            summaryJson = objectMapper.writeValueAsString(summaryMap);
        } catch (Exception e) {
            log.error("Failed to serialize daily challenge summary", e);
        }

        StudySession session = StudySession.builder()
                .account(account)
                .sessionType("DAILY")
                .startedAt(Instant.now())
                .endedAt(Instant.now())
                .summaryJson(summaryJson)
                .build();

        session = studySessionRepository.save(session);

        // 4. Kiểm tra hoàn thành tất cả thử thách trong ngày để tặng +50 XP
        List<ChallengeBank> todayChallenges = getDailyChallenges();
        Set<UUID> todayChallengeIds = todayChallenges.stream()
                .map(ChallengeBank::getId)
                .collect(Collectors.toSet());

        // Lấy tất cả daily sessions hôm nay của account
        List<StudySession> userDailySessions = studySessionRepository.findByAccountIdAndSessionType(account.getId(), "DAILY");
        LocalDate today = LocalDate.now();

        Set<UUID> completedChallengeIdsBefore = new HashSet<>();
        Set<UUID> completedChallengeIdsAfter = new HashSet<>();

        for (StudySession s : userDailySessions) {
            LocalDate sessionDate = LocalDate.ofInstant(s.getStartedAt(), java.time.ZoneId.systemDefault());
            if (sessionDate.equals(today)) {
                try {
                    Map<String, Object> summary = objectMapper.readValue(
                            s.getSummaryJson(), new TypeReference<Map<String, Object>>() {});
                    UUID cid = UUID.fromString((String) summary.get("challengeId"));
                    boolean wasCorrect = Boolean.TRUE.equals(summary.get("isCorrect"));
                    
                    if (wasCorrect && todayChallengeIds.contains(cid)) {
                        if (s.getId() != null && s.getId().equals(session.getId())) {
                            completedChallengeIdsAfter.add(cid);
                        } else {
                            completedChallengeIdsBefore.add(cid);
                            completedChallengeIdsAfter.add(cid);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse study session summary JSON: {}", e.getMessage());
                }
            }
        }

        if (isCorrect && todayChallengeIds.contains(challengeId)) {
            completedChallengeIdsAfter.add(challengeId);
        }

        boolean completedAllToday = completedChallengeIdsAfter.size() >= todayChallengeIds.size() && todayChallengeIds.size() > 0;
        boolean completedAllTodayBefore = completedChallengeIdsBefore.size() >= todayChallengeIds.size() && todayChallengeIds.size() > 0;

        int xpAwarded = 0;
        if (completedAllToday && !completedAllTodayBefore) {
            xpAwarded = 50;
            account.setTotalExperience((account.getTotalExperience() != null ? account.getTotalExperience() : 0) + xpAwarded);
            accountRepository.save(account);
            log.info("User {} completed all daily challenges! Awarded 50 XP. New XP: {}", account.getEmail(), account.getTotalExperience());

            // Tự động quét và mở khóa huy hiệu
            badgeUnlockService.checkAndUnlockBadges(account);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("evaluation", evaluation);
        result.put("audioUrl", audioUrl);
        result.put("completedAllToday", completedAllToday);
        result.put("xpAwarded", xpAwarded);
        result.put("currentCompletedCount", completedChallengeIdsAfter.size());
        result.put("totalChallengesCount", todayChallengeIds.size());

        return result;
    }
}
