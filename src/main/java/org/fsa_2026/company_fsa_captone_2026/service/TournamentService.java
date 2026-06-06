package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.AccountReward;
import org.fsa_2026.company_fsa_captone_2026.entity.RewardCatalog;
import org.fsa_2026.company_fsa_captone_2026.entity.Tournament;
import org.fsa_2026.company_fsa_captone_2026.entity.TournamentParticipant;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRewardRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.RewardCatalogRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.TournamentParticipantRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.TournamentRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;
    private final AccountRepository accountRepository;
    private final RewardCatalogRepository rewardCatalogRepository;
    private final AccountRewardRepository accountRewardRepository;
    private final BadgeUnlockService badgeUnlockService;
    private final ChallengeBankRepository challengeBankRepository;
    private final ObjectMapper objectMapper;

    /**
     * Chốt giải đấu tuần hiện tại, trao giải thưởng XP và huy hiệu cho Top 3, 
     * đồng thời tạo và kích hoạt giải đấu cho tuần kế tiếp với bộ câu hỏi tuần mới.
     */
    @Transactional
    public Map<String, Object> finalizeWeeklyTournament(UUID tournamentId) {
        Tournament tournament = null;
        if (tournamentId != null) {
            tournament = tournamentRepository.findById(tournamentId)
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy giải đấu"));
        } else {
            // Lấy giải đấu tuần ACTIVE đầu tiên
            List<Tournament> activeWeeklies = tournamentRepository.findByTypeAndStatus("WEEKLY", "ACTIVE");
            if (!activeWeeklies.isEmpty()) {
                tournament = activeWeeklies.get(0);
            }
        }

        if (tournament == null) {
            log.info("No active weekly tournament found to finalize. Finalizing based on Global Stars Leaderboard.");
        } else {
            if ("FINISHED".equalsIgnoreCase(tournament.getStatus())) {
                throw new ApiException("BAD_REQUEST", "Giải đấu này đã được chốt từ trước.");
            }
            
            // Validate: Không được phép chốt nếu chưa hết thời gian thi đấu!
            if (Instant.now().isBefore(tournament.getEndsAt())) {
                throw new ApiException("BAD_REQUEST", "Giải đấu này chưa kết thúc thời gian thi đấu (Kết thúc lúc: " 
                        + tournament.getEndsAt() + "). Không thể chốt sớm để đảm bảo công bằng và quyền lợi của học viên.");
            }
            
            tournament.setStatus("FINISHED");
            tournamentRepository.save(tournament);
            log.info("Weekly tournament '{}' is marked as FINISHED.", tournament.getName());
        }

        // Lấy Top 3 người chiến thắng
        List<Account> winners = new ArrayList<>();
        if (tournament != null) {
            List<TournamentParticipant> participants = tournamentParticipantRepository
                    .findByTournamentIdOrderByTotalXpDesc(tournament.getId());
            for (TournamentParticipant p : participants) {
                if (winners.size() < 3) {
                    winners.add(p.getAccount());
                }
            }
        }

        // Fallback: nếu không có giải đấu hoặc giải đấu không có người tham gia, dùng Global Stars Leaderboard
        if (winners.isEmpty()) {
            log.info("Fallback to Global Stars Leaderboard for top 3 users.");
            List<Account> activeUsers = accountRepository
                    .findTop50ByIsActiveTrueOrderByTotalStarsDescBadgeCountDescCurrentStreakDaysDesc();
            for (Account user : activeUsers) {
                if (winners.size() < 3) {
                    winners.add(user);
                }
            }
        }

        // Trao giải thưởng cho Top 3
        int[] xpPrizes = {500, 250, 100};
        String[] titles = {"Quán Quân", "Á Quân", "Hạng Ba"};
        
        for (int i = 0; i < winners.size(); i++) {
            Account user = winners.get(i);
            int xpReward = xpPrizes[i];
            user.setTotalExperience((user.getTotalExperience() != null ? user.getTotalExperience() : 0) + xpReward);
            accountRepository.save(user);
            log.info("Awarded {} XP to {} ({}) as {}", xpReward, user.getFullName(), user.getEmail(), titles[i]);
            
            // Quét badge sau khi cộng XP
            badgeUnlockService.checkAndUnlockBadges(user);
        }

        // Trao cúp vô địch (Badge TOURNAMENT_WINNER) cho Quán Quân (Top 1)
        if (!winners.isEmpty()) {
            Account champion = winners.get(0);
            Optional<RewardCatalog> winnerBadgeOpt = rewardCatalogRepository.findByCode("TOURNAMENT_WINNER");
            if (winnerBadgeOpt.isPresent()) {
                RewardCatalog winnerBadge = winnerBadgeOpt.get();
                Optional<AccountReward> existingCup = accountRewardRepository
                        .findByAccountIdAndRewardCatalogId(champion.getId(), winnerBadge.getId());
                if (existingCup.isEmpty()) {
                    AccountReward cupReward = AccountReward.builder()
                            .account(champion)
                            .rewardCatalog(winnerBadge)
                            .status("UNLOCKED")
                            .progressValue(100)
                            .unlockedAt(Instant.now())
                            .build();
                    accountRewardRepository.save(cupReward);
                    
                    champion.setBadgeCount((champion.getBadgeCount() != null ? champion.getBadgeCount() : 0) + 1);
                    accountRepository.save(champion);
                    log.info("Awarded TOURNAMENT_WINNER badge to Champion: {}", champion.getEmail());
                }
            }
        }

        // Trao huy chương tham chiến (Badge TOURNAMENT_PARTICIPANT) cho Top 3
        Optional<RewardCatalog> partBadgeOpt = rewardCatalogRepository.findByCode("TOURNAMENT_PARTICIPANT");
        if (partBadgeOpt.isPresent()) {
            RewardCatalog partBadge = partBadgeOpt.get();
            for (Account user : winners) {
                Optional<AccountReward> existingPart = accountRewardRepository
                        .findByAccountIdAndRewardCatalogId(user.getId(), partBadge.getId());
                if (existingPart.isEmpty()) {
                    AccountReward partReward = AccountReward.builder()
                            .account(user)
                            .rewardCatalog(partBadge)
                            .status("UNLOCKED")
                            .progressValue(100)
                            .unlockedAt(Instant.now())
                            .build();
                    accountRewardRepository.save(partReward);
                    
                    user.setBadgeCount((user.getBadgeCount() != null ? user.getBadgeCount() : 0) + 1);
                    accountRepository.save(user);
                    log.info("Awarded TOURNAMENT_PARTICIPANT badge to user: {}", user.getEmail());
                }
            }
        }

        // Tự động xoay tua sang Giải Đấu mới cho tuần kế tiếp (ưu tiên giải đấu UPCOMING được tạo sẵn)
        List<Tournament> upcomingTournaments = tournamentRepository.findByTypeAndStatusOrderByStartsAtAsc("WEEKLY", "UPCOMING");
        Tournament nextTournament;
        if (!upcomingTournaments.isEmpty()) {
            nextTournament = upcomingTournaments.get(0);
            nextTournament.setStatus("ACTIVE");
            if (nextTournament.getQuestionsJson() == null || nextTournament.getQuestionsJson().isBlank()) {
                generateWeeklyQuestions(nextTournament);
            }
            nextTournament = tournamentRepository.save(nextTournament);
            log.info("Activated pre-created upcoming weekly tournament: {}", nextTournament.getName());
        } else {
            String nextWeekDate = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toString();
            nextTournament = Tournament.builder()
                    .name("Giải Đấu Tuần - Bắt đầu từ " + nextWeekDate)
                    .description("Giải đấu thi đua hàng tuần dành cho tất cả học viên SpeakVN Journey.")
                    .type("WEEKLY")
                    .status("ACTIVE")
                    .startsAt(Instant.now())
                    .endsAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();
            
            nextTournament = tournamentRepository.save(nextTournament);
            generateWeeklyQuestions(nextTournament);
            nextTournament = tournamentRepository.save(nextTournament);
            log.info("Created and activated generic new weekly tournament: {}", nextTournament.getName());
        }

        Map<String, Object> report = new HashMap<>();
        report.put("finalizedTournament", tournament != null ? tournament.getName() : "None (Global Fallback)");
        report.put("champion", !winners.isEmpty() ? winners.get(0).getFullName() + " (" + winners.get(0).getEmail() + ")" : "N/A");
        report.put("runnerUp", winners.size() > 1 ? winners.get(1).getFullName() + " (" + winners.get(1).getEmail() + ")" : "N/A");
        report.put("thirdPlace", winners.size() > 2 ? winners.get(2).getFullName() + " (" + winners.get(2).getEmail() + ")" : "N/A");
        report.put("newTournament", nextTournament.getName());
        report.put("newTournamentStartsAt", nextTournament.getStartsAt());
        report.put("newTournamentEndsAt", nextTournament.getEndsAt());
        report.put("message", "Chốt giải đấu và trao thưởng thành công, giải đấu mới đã được kích hoạt.");

        return report;
    }

    /**
     * Lấy giải tuần đang kích hoạt. Nếu chưa có, tự động tạo mới giải đấu và sinh câu hỏi.
     */
    @Transactional
    public Tournament getOrCreateActiveTournament() {
        List<Tournament> activeWeeklies = tournamentRepository.findByTypeAndStatus("WEEKLY", "ACTIVE");
        if (!activeWeeklies.isEmpty()) {
            Tournament active = activeWeeklies.get(0);
            if (active.getQuestionsJson() == null || active.getQuestionsJson().isBlank()) {
                generateWeeklyQuestions(active);
                active = tournamentRepository.save(active);
            } else {
                try {
                    List<UUID> currentIds = objectMapper.readValue(
                            active.getQuestionsJson(),
                            new TypeReference<List<UUID>>() {}
                    );
                    if (currentIds.size() < 10) {
                        List<ChallengeBank> allChallenges = challengeBankRepository.findBySkillType(org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType.SPEAKING);
                        List<UUID> existingIds = new ArrayList<>(currentIds);
                        for (ChallengeBank c : allChallenges) {
                            if (existingIds.size() >= 10) break;
                            if (!existingIds.contains(c.getId())) {
                                existingIds.add(c.getId());
                            }
                        }
                        active.setQuestionsJson(objectMapper.writeValueAsString(existingIds));
                        active = tournamentRepository.save(active);
                        log.info("Successfully topped up active tournament to 10 questions: {}", active.getName());
                    }
                } catch (Exception e) {
                    log.error("Failed to parse or top up active questions_json", e);
                }
            }
            return active;
        }

        // Ưu tiên giải đấu UPCOMING được tạo sẵn
        List<Tournament> upcomingTournaments = tournamentRepository.findByTypeAndStatusOrderByStartsAtAsc("WEEKLY", "UPCOMING");
        if (!upcomingTournaments.isEmpty()) {
            Tournament nextTournament = upcomingTournaments.get(0);
            nextTournament.setStatus("ACTIVE");
            if (nextTournament.getQuestionsJson() == null || nextTournament.getQuestionsJson().isBlank()) {
                generateWeeklyQuestions(nextTournament);
            }
            nextTournament = tournamentRepository.save(nextTournament);
            log.info("Activated pre-created upcoming weekly tournament: {}", nextTournament.getName());
            return nextTournament;
        }

        String nextWeekDate = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toString();
        Tournament nextTournament = Tournament.builder()
                .name("Giải Đấu Tuần - Bắt đầu từ " + nextWeekDate)
                .description("Giải đấu thi đua hàng tuần dành cho tất cả học viên SpeakVN Journey.")
                .type("WEEKLY")
                .status("ACTIVE")
                .startsAt(Instant.now())
                .endsAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        nextTournament = tournamentRepository.save(nextTournament);
        generateWeeklyQuestions(nextTournament);
        nextTournament = tournamentRepository.save(nextTournament);
        log.info("Created and activated new weekly tournament with questions: {}", nextTournament.getName());
        return nextTournament;
    }

    /**
     * Sinh bộ câu hỏi ngẫu nhiên 10 câu từ ChallengeBank cho giải đấu tuần (chỉ lấy Speaking).
     */
    private void generateWeeklyQuestions(Tournament tournament) {
        List<ChallengeBank> allChallenges = challengeBankRepository.findBySkillType(org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType.SPEAKING);
        if (allChallenges.isEmpty()) {
            log.warn("Challenge bank is empty for SPEAKING skill. Cannot generate weekly questions.");
            return;
        }
        List<ChallengeBank> copy = new ArrayList<>(allChallenges);
        Collections.shuffle(copy);
        List<UUID> selectedIds = copy.stream()
                .limit(10)
                .map(ChallengeBank::getId)
                .collect(Collectors.toList());

        try {
            tournament.setQuestionsJson(objectMapper.writeValueAsString(selectedIds));
            log.info("Successfully generated 10 weekly questions for tournament: {}", tournament.getName());
        } catch (Exception e) {
            log.error("Failed to serialize weekly questions to JSON", e);
        }
    }

    /**
     * Lấy chi tiết giải đấu tuần đang hoạt động kèm thông tin bộ câu hỏi và tiến trình của học viên.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getActiveTournamentDetails(String email) {
        // Tránh lazy initialization hoặc đệ quy: chạy getOrCreateActiveTournament() ngoài transactional readOnly nếu cần,
        // hoặc xử lý an toàn: phương thức này có thể chạy read/write transactional bình thường.
        Tournament tournament = getOrCreateActiveTournament();
        
        List<UUID> questionIds = new ArrayList<>();
        if (tournament.getQuestionsJson() != null && !tournament.getQuestionsJson().isBlank()) {
            try {
                questionIds = objectMapper.readValue(
                        tournament.getQuestionsJson(),
                        new TypeReference<List<UUID>>() {}
                );
            } catch (Exception e) {
                log.error("Failed to parse tournament questions_json", e);
            }
        }

        List<ChallengeBank> challenges = challengeBankRepository.findAllById(questionIds);
        
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người học"));

        TournamentParticipant participant = tournamentParticipantRepository
                .findByTournamentIdAndAccountId(tournament.getId(), account.getId())
                .orElse(null);

        Map<String, Object> rawScores = new HashMap<>();
        if (participant != null && participant.getScoresJson() != null && !participant.getScoresJson().isBlank()) {
            try {
                rawScores = objectMapper.readValue(
                        participant.getScoresJson(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } catch (Exception e) {
                log.error("Failed to parse participant scores_json", e);
            }
        }

        List<Map<String, Object>> challengeList = new ArrayList<>();
        for (ChallengeBank c : challenges) {
            Map<String, Object> cMap = new HashMap<>();
            cMap.put("id", c.getId());
            cMap.put("contentText", c.getContentText());
            cMap.put("skillType", c.getSkillType());
            cMap.put("region", c.getRegion());
            cMap.put("metadataJson", c.getMetadataJson());
            
            Object val = rawScores.get(c.getId().toString());
            int userHighScore = 0;
            if (val instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) val;
                Number pts = (Number) map.get("points");
                userHighScore = pts != null ? pts.intValue() : 0;
            } else if (val instanceof Number) {
                userHighScore = ((Number) val).intValue();
            }
            cMap.put("userHighScore", userHighScore);
            challengeList.add(cMap);
        }

        Map<String, Object> details = new HashMap<>();
        details.put("id", tournament.getId());
        details.put("name", tournament.getName());
        details.put("description", tournament.getDescription());
        details.put("status", tournament.getStatus());
        details.put("endsAt", tournament.getEndsAt());
        details.put("startsAt", tournament.getStartsAt());
        details.put("challenges", challengeList);
        details.put("userProgress", participant != null ? Map.of(
                "totalXp", participant.getTotalXp(),
                "challengesCompleted", participant.getChallengesCompleted(),
                "averageScore", participant.getAverageScore()
        ) : Map.of(
                "totalXp", 0,
                "challengesCompleted", 0,
                "averageScore", 0
        ));

        return details;
    }

    /**
     * Học viên nộp điểm số luyện tập của mình trong giải đấu.
     */
    @Transactional
    public Map<String, Object> submitTournamentScore(String email, UUID challengeId, double score) {
        Tournament tournament = getOrCreateActiveTournament();
        
        List<UUID> questionIds = new ArrayList<>();
        if (tournament.getQuestionsJson() != null && !tournament.getQuestionsJson().isBlank()) {
            try {
                questionIds = objectMapper.readValue(
                        tournament.getQuestionsJson(),
                        new TypeReference<List<UUID>>() {}
                );
            } catch (Exception e) {
                log.error("Failed to parse tournament questions_json", e);
            }
        }

        if (!questionIds.contains(challengeId)) {
            throw new ApiException("BAD_REQUEST", "Câu hỏi này không nằm trong bộ câu hỏi của Giải đấu tuần này.");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người học"));

        TournamentParticipant participant = tournamentParticipantRepository
                .findByTournamentIdAndAccountId(tournament.getId(), account.getId())
                .orElseGet(() -> {
                    TournamentParticipant newPart = TournamentParticipant.builder()
                            .tournament(tournament)
                            .account(account)
                            .totalXp(0)
                            .challengesCompleted(0)
                            .averageScore(BigDecimal.ZERO)
                            .status("ACTIVE")
                            .build();
                    return tournamentParticipantRepository.save(newPart);
                });

        Map<String, Object> rawScores = new HashMap<>();
        if (participant.getScoresJson() != null && !participant.getScoresJson().isBlank()) {
            try {
                rawScores = objectMapper.readValue(
                        participant.getScoresJson(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } catch (Exception e) {
                log.error("Failed to parse participant scores_json", e);
            }
        }

        String challengeKey = challengeId.toString();
        if (rawScores.containsKey(challengeKey)) {
            throw new ApiException("CONFLICT", "Câu hỏi này đã được chấm điểm trước đó và không thể làm lại.");
        }

        int points = (int) Math.round(score);

        Map<String, Object> scoreDetail = new HashMap<>();
        scoreDetail.put("percent", score);
        scoreDetail.put("points", points);
        scoreDetail.put("scoredAt", Instant.now().toString());

        rawScores.put(challengeKey, scoreDetail);

        try {
            participant.setScoresJson(objectMapper.writeValueAsString(rawScores));
        } catch (Exception e) {
            log.error("Failed to serialize participant scores_json", e);
        }

        double totalPoints = 0;
        double totalPercent = 0;
        for (Object val : rawScores.values()) {
            if (val instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) val;
                Number pts = (Number) map.get("points");
                Number pct = (Number) map.get("percent");
                totalPoints += pts != null ? pts.doubleValue() : 0;
                totalPercent += pct != null ? pct.doubleValue() : 0;
            } else if (val instanceof Number) {
                double v = ((Number) val).doubleValue();
                totalPoints += Math.round(v);
                totalPercent += v;
            }
        }

        participant.setTotalXp((int) totalPoints);
        participant.setChallengesCompleted(rawScores.size());
        participant.setAverageScore(BigDecimal.valueOf(totalPercent / rawScores.size()));

        tournamentParticipantRepository.save(participant);

        Map<String, Object> result = new HashMap<>();
        result.put("updated", true);
        result.put("score", points);
        result.put("previousHighScore", 0);
        result.put("totalXp", participant.getTotalXp());
        result.put("challengesCompleted", participant.getChallengesCompleted());
        result.put("averageScore", participant.getAverageScore());
        return result;
    }

    /**
     * Lấy bảng xếp hạng tuần hiện tại.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveTournamentLeaderboard() {
        Tournament tournament = getOrCreateActiveTournament();
        List<TournamentParticipant> participants = tournamentParticipantRepository
                .findByTournamentIdOrderByTotalXpDesc(tournament.getId());

        List<Map<String, Object>> leaderboard = new ArrayList<>();
        int rank = 1;
        for (TournamentParticipant p : participants) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("rankPosition", rank++);
            entry.put("accountId", p.getAccount().getId());
            entry.put("fullName", p.getAccount().getFullName());
            entry.put("avatarUrl", p.getAccount().getAvatarUrl());
            entry.put("totalXp", p.getTotalXp());
            entry.put("challengesCompleted", p.getChallengesCompleted());
            entry.put("averageScore", p.getAverageScore());
            leaderboard.add(entry);
        }
        return leaderboard;
    }

    /**
     * Cập nhật thông tin của giải đấu đang hoạt động.
     */
    @Transactional
    public Tournament updateActiveTournament(String name, String description, Instant endsAt) {
        Tournament tournament = getOrCreateActiveTournament();
        if ("ACTIVE".equalsIgnoreCase(tournament.getStatus())) {
            throw new ApiException("BAD_REQUEST", "Giải đấu đang diễn ra (ACTIVE). Không thể chỉnh sửa thông tin để bảo vệ quyền lợi của các học viên đang thi đấu.");
        }
        if (name != null && !name.isBlank()) {
            tournament.setName(name);
        }
        if (description != null && !description.isBlank()) {
            tournament.setDescription(description);
        }
        if (endsAt != null) {
            tournament.setEndsAt(endsAt);
        }
        return tournamentRepository.save(tournament);
    }

    /**
     * Gán thủ công danh sách câu hỏi phát âm từ ChallengeBank cho giải đấu đang hoạt động.
     */
    @Transactional
    public Tournament assignActiveTournamentQuestions(List<UUID> questionIds) {
        Tournament tournament = getOrCreateActiveTournament();
        if ("ACTIVE".equalsIgnoreCase(tournament.getStatus())) {
            throw new ApiException("BAD_REQUEST", "Giải đấu đang diễn ra (ACTIVE). Không thể thay đổi bộ câu hỏi thi đấu để bảo vệ quyền lợi của các học viên.");
        }
        if (questionIds == null || questionIds.isEmpty()) {
            throw new ApiException("BAD_REQUEST", "Danh sách câu hỏi không được trống.");
        }

        List<ChallengeBank> challenges = challengeBankRepository.findAllById(questionIds);
        if (challenges.size() != questionIds.size()) {
            throw new ApiException("NOT_FOUND", "Có câu hỏi không tồn tại trong ngân hàng câu hỏi.");
        }

        for (ChallengeBank c : challenges) {
            if (c.getSkillType() != org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType.SPEAKING) {
                throw new ApiException("BAD_REQUEST", "Tất cả câu hỏi giải đấu phải là kỹ năng phát âm (SPEAKING).");
            }
        }

        try {
            tournament.setQuestionsJson(objectMapper.writeValueAsString(questionIds));
        } catch (Exception e) {
            log.error("Failed to serialize manual tournament questions to JSON", e);
            throw new ApiException("INTERNAL_ERROR", "Không thể lưu danh sách câu hỏi.");
        }

        return tournamentRepository.save(tournament);
    }

    /**
     * Lấy danh sách lịch sử giải đấu đã kết thúc (FINISHED) kèm thông tin Top 3 người dẫn đầu.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFinishedTournamentsHistory() {
        List<Tournament> finishedTournaments = tournamentRepository.findByTypeAndStatusOrderByEndsAtDesc("WEEKLY", "FINISHED");
        List<Map<String, Object>> history = new ArrayList<>();

        for (Tournament t : finishedTournaments) {
            Map<String, Object> tMap = new HashMap<>();
            tMap.put("id", t.getId());
            tMap.put("name", t.getName());
            tMap.put("description", t.getDescription());
            tMap.put("startsAt", t.getStartsAt());
            tMap.put("endsAt", t.getEndsAt());
            tMap.put("status", t.getStatus());

            List<TournamentParticipant> participants = tournamentParticipantRepository
                    .findByTournamentIdOrderByTotalXpDesc(t.getId());

            List<Map<String, Object>> winners = new ArrayList<>();
            int rank = 1;
            for (TournamentParticipant p : participants) {
                if (winners.size() < 3) {
                    Map<String, Object> wMap = new HashMap<>();
                    wMap.put("rankPosition", rank++);
                    wMap.put("fullName", p.getAccount().getFullName());
                    wMap.put("email", p.getAccount().getEmail());
                    wMap.put("avatarUrl", p.getAccount().getAvatarUrl());
                    wMap.put("totalXp", p.getTotalXp());
                    wMap.put("averageScore", p.getAverageScore());
                    winners.add(wMap);
                } else {
                    break;
                }
            }
            tMap.put("winners", winners);
            history.add(tMap);
        }

        return history;
    }

    /**
     * Tạo một giải đấu chuẩn bị diễn ra (UPCOMING) cho tuần sau hoặc tương lai.
     */
    @Transactional
    public Tournament createUpcomingTournament(String name, String description, Instant startsAt, Instant endsAt, List<UUID> questionIds) {
        if (startsAt == null || endsAt == null) {
            throw new ApiException("BAD_REQUEST", "Thời gian bắt đầu và kết thúc không được trống.");
        }
        if (startsAt.isAfter(endsAt)) {
            throw new ApiException("BAD_REQUEST", "Thời gian bắt đầu phải trước thời gian kết thúc.");
        }

        Tournament tournament = Tournament.builder()
                .name(name != null && !name.isBlank() ? name : "Giải Đấu Tuần Mới (Đang chuẩn bị)")
                .description(description != null ? description : "")
                .type("WEEKLY")
                .status("UPCOMING")
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        if (questionIds != null && !questionIds.isEmpty()) {
            List<org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank> challenges = challengeBankRepository.findAllById(questionIds);
            if (challenges.size() != questionIds.size()) {
                throw new ApiException("NOT_FOUND", "Có câu hỏi không tồn tại trong ngân hàng câu hỏi.");
            }
            for (org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank c : challenges) {
                if (c.getSkillType() != org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType.SPEAKING) {
                    throw new ApiException("BAD_REQUEST", "Tất cả câu hỏi giải đấu phải là kỹ năng phát âm (SPEAKING).");
                }
            }
            try {
                tournament.setQuestionsJson(objectMapper.writeValueAsString(questionIds));
            } catch (Exception e) {
                log.error("Failed to serialize upcoming tournament questions", e);
                throw new ApiException("INTERNAL_ERROR", "Không thể lưu danh sách câu hỏi.");
            }
        }

        return tournamentRepository.save(tournament);
    }

    /**
     * Lấy toàn bộ danh sách giải đấu sắp xếp theo thời gian bắt đầu giảm dần.
     */
    @Transactional(readOnly = true)
    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "startsAt"));
    }

    /**
     * Lấy bảng xếp hạng của một giải đấu bất kỳ dựa trên ID.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTournamentLeaderboard(UUID tournamentId) {
        List<TournamentParticipant> participants = tournamentParticipantRepository
                .findByTournamentIdOrderByTotalXpDesc(tournamentId);

        List<Map<String, Object>> leaderboard = new ArrayList<>();
        int rank = 1;
        for (TournamentParticipant p : participants) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("rankPosition", rank++);
            entry.put("accountId", p.getAccount().getId());
            entry.put("fullName", p.getAccount().getFullName());
            entry.put("avatarUrl", p.getAccount().getAvatarUrl());
            entry.put("totalXp", p.getTotalXp());
            entry.put("challengesCompleted", p.getChallengesCompleted());
            entry.put("averageScore", p.getAverageScore());
            leaderboard.add(entry);
        }
        return leaderboard;
    }
}

