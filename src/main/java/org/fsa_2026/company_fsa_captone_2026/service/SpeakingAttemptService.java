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
            int geminiScore,
            boolean isCorrect,
            String dialect) {

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
            SpeakingAttempt attempt = SpeakingAttempt.builder()
                    .account(accountOpt.get())
                    .challenge(challenge)
                    .targetText(targetText)
                    .asrTranscription(asrTranscription)
                    .audioUrl(audioUrl)
                    .geminiScore(geminiScore)
                    .isCorrect(isCorrect)
                    .dialect(dialect)
                    .consentGiven(true)
                    .build();

            speakingAttemptRepository.save(attempt);
            log.info("[SpeakingAttempt] Saved attempt for account: {}, score: {}", accountEmail, geminiScore);

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
}
