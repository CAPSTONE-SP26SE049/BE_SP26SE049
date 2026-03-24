package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.EntrytestRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.EntrytestResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.StudySession;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.StudySessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntrytestService {

    private final AccountRepository accountRepository;
    private final StudySessionRepository studySessionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Evaluate audio for accent classification.
     * Mocked for now, will call AI later.
     */
    @Transactional
    public EntrytestResponse evaluateAccent(String email, EntrytestRequest request) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        // Mocking AI Classification response
        String[] regions = {"BAC", "TRUNG", "NAM"};
        int randomIndex = (int) (Math.random() * regions.length);
        String suggestedRegion = regions[randomIndex];
        BigDecimal confidence = BigDecimal.valueOf(0.75 + (Math.random() * 0.25)); // 0.75 - 1.0

        String regionVN = suggestedRegion.equals("BAC") ? "Miền Bắc" :
                          suggestedRegion.equals("TRUNG") ? "Miền Trung" : "Miền Nam";

        String feedback = String.format("Hệ thống nhận diện giọng bạn giống vùng: %s. Độ chính xác: %s%%", 
                regionVN, confidence.multiply(BigDecimal.valueOf(100)).intValue());

        // Log to StudySession for analytics
        try {
            Map<String, Object> summary = new HashMap<>();
            summary.put("audioUrl", request.getAudioUrl());
            summary.put("suggestedRegion", suggestedRegion);
            summary.put("confidence", confidence);

            StudySession session = StudySession.builder()
                    .account(account)
                    .sessionType("ENTRYTEST")
                    .startedAt(Instant.now())
                    .endedAt(Instant.now())
                    .summaryJson(objectMapper.writeValueAsString(summary))
                    .build();

            studySessionRepository.save(session);
        } catch (Exception e) {
            log.warn("Failed to log entrytest session metadata", e);
        }

        return EntrytestResponse.builder()
                .suggestedRegion(suggestedRegion)
                .confidence(confidence)
                .feedback(feedback)
                .build();
    }

    /**
     * Confirm/update user selected region.
     */
    @Transactional
    public void updateUserRegion(String email, String selectedRegion) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        // Support validation on region if needed (BAC, TRUNG, NAM)
        account.setRegion(selectedRegion.toUpperCase());
        accountRepository.save(account);
        log.info("Updated region to {} for user {}", selectedRegion, email);
    }
}
