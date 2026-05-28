package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.StudySession;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem;
import org.fsa_2026.company_fsa_captone_2026.entity.CustomPathLevel;
import org.fsa_2026.company_fsa_captone_2026.entity.DailyChallengeAttempt;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.StudySessionRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.QuizChallengeItemRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.CustomLearningPathRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.DailyChallengeAttemptRepository;
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

    private final LearningUnitRepository learningUnitRepository;
    private final AccountLearningUnitRepository accountLearningUnitRepository;
    private final QuizChallengeItemRepository quizChallengeItemRepository;
    private final CustomLearningPathRepository customLearningPathRepository;
    private final DailyChallengeAttemptRepository dailyChallengeAttemptRepository;

    /**
     * Lấy 3 câu hỏi thử thách của hôm nay (mặc định toàn cục). Tự động xoay tua nếu sang ngày mới.
     */
    @Transactional
    public List<ChallengeBank> getDailyChallenges() {
        return getDailyChallenges(null);
    }

    /**
     * Lấy 3 câu hỏi thử thách phát âm cá nhân hóa của hôm nay dựa theo tiến độ học tập (email).
     */
    @Transactional
    public List<ChallengeBank> getDailyChallenges(String email) {
        if (email == null || email.isBlank()) {
            return getGlobalDailyChallenges();
        }

        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) {
            return getGlobalDailyChallenges();
        }

        Account account = accountOpt.get();

        // 1. Xác định dialect tương ứng vùng miền của học viên
        List<LearningUnit> dialects = learningUnitRepository.findByType("DIALECT");
        LearningUnit matchedDialect = null;
        String region = account.getRegion();
        if (region != null) {
            String upperRegion = region.toUpperCase();
            for (LearningUnit d : dialects) {
                String nameUpper = d.getName().toUpperCase();
                if (upperRegion.contains("NORTH") || upperRegion.contains("BAC")) {
                    if (nameUpper.contains("BẮC") || nameUpper.contains("NORTH") || nameUpper.contains("BAC")) {
                        matchedDialect = d;
                        break;
                    }
                } else if (upperRegion.contains("CENTRAL") || upperRegion.contains("TRUNG")) {
                    if (nameUpper.contains("TRUNG") || nameUpper.contains("CENTRAL")) {
                        matchedDialect = d;
                        break;
                    }
                } else if (upperRegion.contains("SOUTH") || upperRegion.contains("NAM")) {
                    if (nameUpper.contains("NAM") || nameUpper.contains("SOUTH")) {
                        matchedDialect = d;
                        break;
                    }
                }
            }
        }
        if (matchedDialect == null && !dialects.isEmpty()) {
            matchedDialect = dialects.get(0);
        }

        // 2. Lấy thông tin tiến độ đã lưu
        List<AccountLearningUnit> progressList = accountLearningUnitRepository.findByAccountId(account.getId());
        Map<UUID, AccountLearningUnit> progressMap = progressList.stream()
                .collect(Collectors.toMap(
                        al -> al.getLearningUnit().getId(),
                        al -> al,
                        (existing, replacement) -> existing
                ));

        LearningUnit activeLevel = null;

        // 3. Nếu đang có Lộ trình cá nhân hóa CustomLearningPath, tìm level đầu tiên chưa hoàn thành
        var customPathOpt = customLearningPathRepository.findFirstByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(account.getId());
        if (customPathOpt.isPresent()) {
            List<CustomPathLevel> customLevels = customPathOpt.get().getLevels();
            for (CustomPathLevel pl : customLevels) {
                AccountLearningUnit progress = progressMap.get(pl.getLevel().getId());
                if (progress == null || !Boolean.TRUE.equals(progress.getIsCompleted())) {
                    activeLevel = pl.getLevel();
                    break;
                }
            }
            if (activeLevel == null && !customLevels.isEmpty()) {
                activeLevel = customLevels.get(customLevels.size() - 1).getLevel();
            }
        }

        // 4. Nếu học theo Lộ trình vùng miền thông thường, tìm level đầu tiên chưa hoàn thành
        if (activeLevel == null && matchedDialect != null) {
            List<LearningUnit> allLevels = new ArrayList<>();
            fetchAllDescendantLevels(matchedDialect.getId(), allLevels);
            allLevels.sort(Comparator.comparingInt(this::extractLevelOrderFromMetadata));
            for (LearningUnit lvl : allLevels) {
                AccountLearningUnit progress = progressMap.get(lvl.getId());
                if (progress == null || !Boolean.TRUE.equals(progress.getIsCompleted())) {
                    activeLevel = lvl;
                    break;
                }
            }
            if (activeLevel == null && !allLevels.isEmpty()) {
                activeLevel = allLevels.get(allLevels.size() - 1);
            }
        }

        // 5. Thu thập toàn bộ các câu hỏi SPEAKING từ Level hoạt động hiện tại
        List<ChallengeBank> speakingChallenges = new ArrayList<>();
        String regionFilter = "BAC"; // Mặc định vùng miền
        
        if (activeLevel != null) {
            // Tra cứu vùng miền từ Dialect cha của level
            LearningUnit current = activeLevel;
            while (current != null) {
                if ("DIALECT".equalsIgnoreCase(current.getType())) {
                    String nameUpper = current.getName().toUpperCase();
                    if (nameUpper.contains("BẮC") || nameUpper.contains("NORTH") || nameUpper.contains("BAC")) {
                        regionFilter = "BAC";
                    } else if (nameUpper.contains("TRUNG") || nameUpper.contains("CENTRAL")) {
                        regionFilter = "TRUNG";
                    } else if (nameUpper.contains("NAM") || nameUpper.contains("SOUTH")) {
                        regionFilter = "NAM";
                    }
                    break;
                }
                current = current.getParent();
            }

            List<LearningUnit> quizzes = learningUnitRepository.findByParentAndType(activeLevel, "QUIZ");
            List<UUID> quizIds = quizzes.stream().map(LearningUnit::getId).collect(Collectors.toList());
            List<QuizChallengeItem> challengeItems = new ArrayList<>();
            for (UUID qid : quizIds) {
                challengeItems.addAll(quizChallengeItemRepository.findByQuizIdOrderByOrderIndex(qid));
            }

            List<UUID> challengeIds = challengeItems.stream()
                    .map(item -> item.getChallengeBankId() != null ? item.getChallengeBankId() : item.getChallengeId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            for (UUID cid : challengeIds) {
                challengeBankRepository.findById(cid).ifPresent(c -> {
                    if (c.getSkillType() == org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType.SPEAKING) {
                        speakingChallenges.add(c);
                    }
                });
            }
        } else if (matchedDialect != null) {
            String nameUpper = matchedDialect.getName().toUpperCase();
            if (nameUpper.contains("BẮC") || nameUpper.contains("NORTH") || nameUpper.contains("BAC")) {
                regionFilter = "BAC";
            } else if (nameUpper.contains("TRUNG") || nameUpper.contains("CENTRAL")) {
                regionFilter = "TRUNG";
            } else if (nameUpper.contains("NAM") || nameUpper.contains("SOUTH")) {
                regionFilter = "NAM";
            }
        }

        // 6. Bù đắp câu hỏi phát âm từ vùng miền tương ứng nếu số lượng câu hỏi của level < 3
        Set<UUID> presentIds = speakingChallenges.stream().map(ChallengeBank::getId).collect(Collectors.toSet());
        if (speakingChallenges.size() < 3) {
            final String finalReg = regionFilter;
            List<ChallengeBank> regionalFallback = challengeBankRepository.findAll().stream()
                    .filter(c -> c.getSkillType() == org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType.SPEAKING 
                            && finalReg.equalsIgnoreCase(c.getRegion()))
                    .collect(Collectors.toList());

            for (ChallengeBank c : regionalFallback) {
                if (!presentIds.contains(c.getId())) {
                    speakingChallenges.add(c);
                    presentIds.add(c.getId());
                }
            }
        }

        // 7. Tiếp tục bù đắp từ toàn bộ các câu hỏi phát âm hệ thống nếu vẫn < 3
        if (speakingChallenges.size() < 3) {
            List<ChallengeBank> allSpeaking = challengeBankRepository.findBySkillType(org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType.SPEAKING);
            for (ChallengeBank c : allSpeaking) {
                if (!presentIds.contains(c.getId())) {
                    speakingChallenges.add(c);
                    presentIds.add(c.getId());
                }
            }
        }

        // 8. Nếu vẫn rỗng, rơi về nạp ngẫu nhiên toàn cục
        if (speakingChallenges.isEmpty()) {
            return getGlobalDailyChallenges();
        }

        // 9. Sử dụng hạt giống trộn ngẫu nhiên nhất quán theo học viên và ngày hiện tại
        List<ChallengeBank> selected;
        if (speakingChallenges.size() <= 3) {
            selected = speakingChallenges;
        } else {
            long seed = Objects.hash(account.getId(), LocalDate.now().toString());
            Random rand = new Random(seed);
            List<ChallengeBank> shuffled = new ArrayList<>(speakingChallenges);
            Collections.shuffle(shuffled, rand);
            selected = shuffled.subList(0, 3);
        }

        return selected;
    }

    private void fetchAllDescendantLevels(UUID parentId, List<LearningUnit> accumulator) {
        List<LearningUnit> children = learningUnitRepository.findByParentId(parentId);
        for (LearningUnit child : children) {
            if ("LEVEL".equals(child.getType())) {
                accumulator.add(child);
            }
        }
    }

    private int extractLevelOrderFromMetadata(LearningUnit level) {
        if (level.getMetadataJson() == null || level.getMetadataJson().isBlank()) return 0;
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                    level.getMetadataJson(), new TypeReference<Map<String, Object>>() {});
            Object order = metadata.get("level_order");
            if (order == null) order = metadata.get("orderIndex");
            if (order instanceof Number) return ((Number) order).intValue();
            if (order instanceof String) return Integer.parseInt((String) order);
        } catch (Exception ignored) {}
        return 0;
    }

    private List<ChallengeBank> getGlobalDailyChallenges() {
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
     * Nộp ghi âm bài làm daily challenge: bắt buộc .webm (giống Entry Test), chấm AI và trao thưởng.
     */
    @Transactional
    public Map<String, Object> submitDailyChallenge(String email, UUID challengeId, MultipartFile audio, String dialect) throws IOException {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy tài khoản"));

        ChallengeBank challenge = challengeBankRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy thử thách"));

        // 1. Đánh giá phát âm qua AI — validate .webm trước khi đọc bytes (fix D-04)
        String focusErrorTag = null;
        if (dialect != null && !dialect.isBlank()) {
            focusErrorTag = aiService.findErrorTagUnitId(dialect);
        }
        Map<String, Object> evaluation = aiService.evaluatePronunciation(audio, challenge.getContentText(), focusErrorTag);

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

        // 3b. Lưu thông tin vào bảng daily_challenge_attempt chuyên biệt
        DailyChallengeAttempt attempt = DailyChallengeAttempt.builder()
                .account(account)
                .challenge(challenge)
                .isCorrect(isCorrect)
                .score(((Number) evaluation.getOrDefault("accuracy", 0)).intValue())
                .audioUrl(audioUrl)
                .feedback((String) evaluation.getOrDefault("feedback", ""))
                .dialect(dialect)
                .build();
        dailyChallengeAttemptRepository.save(attempt);

        // 4. Kiểm tra hoàn thành tất cả thử thách trong ngày để tặng +50 XP
        List<ChallengeBank> todayChallenges = getDailyChallenges(email);
        Set<UUID> todayChallengeIds = todayChallenges.stream()
                .map(ChallengeBank::getId)
                .collect(Collectors.toSet());

        Instant startOfToday = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        List<DailyChallengeAttempt> todayCorrectAttempts = dailyChallengeAttemptRepository
                .findByAccountIdAndIsCorrectTrueAndCreatedAtGreaterThanEqual(account.getId(), startOfToday);

        Set<UUID> completedChallengeIdsAfter = todayCorrectAttempts.stream()
                .map(a -> a.getChallenge().getId())
                .filter(todayChallengeIds::contains)
                .collect(Collectors.toSet());

        Set<UUID> completedChallengeIdsBefore = todayCorrectAttempts.stream()
                .filter(a -> !a.getChallenge().getId().equals(challengeId))
                .map(a -> a.getChallenge().getId())
                .filter(todayChallengeIds::contains)
                .collect(Collectors.toSet());

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

    /**
     * Lấy danh sách ID của các thử thách phát âm mà người dùng đã hoàn thành thành công hôm nay.
     */
    @Transactional(readOnly = true)
    public List<UUID> getCompletedChallengeIdsToday(String email) {
        if (email == null || email.isBlank()) {
            return Collections.emptyList();
        }
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) {
            return Collections.emptyList();
        }
        Account account = accountOpt.get();
        Instant startOfToday = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        List<DailyChallengeAttempt> todayCorrectAttempts = dailyChallengeAttemptRepository
                .findByAccountIdAndIsCorrectTrueAndCreatedAtGreaterThanEqual(account.getId(), startOfToday);
        return todayCorrectAttempts.stream()
                .map(a -> a.getChallenge().getId())
                .distinct()
                .collect(Collectors.toList());
    }
}
