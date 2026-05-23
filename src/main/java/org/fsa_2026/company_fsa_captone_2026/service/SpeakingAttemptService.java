package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.SpeakingAttempt;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.SpeakingAttemptRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeakingAttemptService {

    private final SpeakingAttemptRepository speakingAttemptRepository;
    private final AccountRepository accountRepository;
    private final ChallengeBankRepository challengeBankRepository;

    /**
     * Lưu attempt sau khi người dùng đã đồng ý.
     * Lưu metadata vào DB (audioUrl đã được frontend upload).
     */
    @Async
    public void saveAttemptAsync(
            String accountEmail,
            String challengeIdStr,
            String targetText,
            String asrTranscription,
            String audioUrl,
            int groqScore,
            boolean isCorrect,
            String dialect,
            Long processingTimeMs,
            Long asrProcessingTimeMs,
            String groqFeedback,
            Integer asrScore,
            String wordDetails,
            String recordId) {

        try {
            // 1. Tìm Account
            Optional<Account> accountOpt = accountRepository.findByEmail(accountEmail);
            if (accountOpt.isEmpty()) {
                log.warn("[SpeakingAttempt] Account not found: {}", accountEmail);
                return;
            }

            // 2. Tìm Challenge (nullable)
            ChallengeBank challenge = null;
            if (challengeIdStr != null && !challengeIdStr.isBlank()) {
                try {
                    UUID challengeId = UUID.fromString(challengeIdStr);
                    challenge = challengeBankRepository.findById(challengeId).orElse(null);
                } catch (IllegalArgumentException e) {
                    log.warn("[SpeakingAttempt] Invalid challengeId: {}", challengeIdStr);
                }
            }

            // 3. Lưu vào DB
            log.info("[SpeakingAttempt] Saving attempt with processingTimeMs={}, asrProcessingTimeMs={}", processingTimeMs, asrProcessingTimeMs);
            SpeakingAttempt attempt = SpeakingAttempt.builder()
                    .account(accountOpt.get())
                    .challenge(challenge)
                    .targetText(targetText)
                    .asrTranscription(asrTranscription)
                    .audioUrl(audioUrl)
                    .groqScore(groqScore)
                    .isCorrect(isCorrect)
                    .dialect(dialect)
                    .consentGiven(true)
                    .processingTimeMs(processingTimeMs)
                    .asrProcessingTimeMs(asrProcessingTimeMs)
                    .groqFeedback(groqFeedback)
                    .asrScore(asrScore)
                    .wordDetails(wordDetails)
                    .recordId(recordId)
                    .build();

            speakingAttemptRepository.save(attempt);
            log.info("[SpeakingAttempt] Saved attempt for account: {}, score: {}", accountEmail, groqScore);

        } catch (Exception e) {
            log.error("[SpeakingAttempt] Failed to save attempt: {}", e.getMessage(), e);
        }
    }

    /**
     * Admin: lấy danh sách attempts có phân trang và lọc theo dialect.
     */
    public Page<SpeakingAttempt> getAttempts(String dialect, Pageable pageable) {
        return speakingAttemptRepository.findAllByDialect(dialect, pageable);
    }

    /**
     * Admin: lấy thống kê tổng hợp.
     */
    public Map<String, Long> getStats() {
        return Map.of(
                "total", speakingAttemptRepository.countByConsentGivenTrue(),
                "north", speakingAttemptRepository.countByDialectAndConsentGivenTrue("NORTH"),
                "central", speakingAttemptRepository.countByDialectAndConsentGivenTrue("CENTRAL"),
                "south", speakingAttemptRepository.countByDialectAndConsentGivenTrue("SOUTH")
        );
    }

    /**
     * Admin: lấy danh sách logs chi tiết cho AI Monitor (50 lượt gần nhất).
     */
    public List<org.fsa_2026.company_fsa_captone_2026.dto.SpeakingAttemptLogResponse> getAiMonitorLogs(int limit) {
        return speakingAttemptRepository.findTopNOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 200))
        ).stream().map(sa -> org.fsa_2026.company_fsa_captone_2026.dto.SpeakingAttemptLogResponse.builder()
                .id(sa.getId())
                .userEmail(sa.getAccount().getEmail())
                .userFullName(sa.getAccount().getFullName())
                .targetText(sa.getTargetText())
                .asrTranscription(sa.getAsrTranscription())
                .audioUrl(sa.getAudioUrl())
                .groqScore(sa.getGroqScore())
                .groqFeedback(sa.getGroqFeedback())
                .isCorrect(sa.getIsCorrect())
                .dialect(sa.getDialect())
                .processingTimeMs(sa.getProcessingTimeMs())
                .asrProcessingTimeMs(sa.getAsrProcessingTimeMs())
                .createdAt(sa.getCreatedAt())
                .build()
        ).toList();
    }
}
