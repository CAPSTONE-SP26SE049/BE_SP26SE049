package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fsa_2026.company_fsa_captone_2026.dto.AttemptRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.AttemptResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.PhonemeFeedbackResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.PracticeSessionResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.*;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GameplayService
 * Đã cập nhật: dùng StudySession + SessionDetail thay cho PracticeSession + Attempt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameplayService {

    private final AccountRepository accountRepository;
    private final StudySessionRepository studySessionRepository;
    private final SessionDetailRepository sessionDetailRepository;
    private final ContentItemRepository contentItemRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PracticeSessionResponse startSession(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        StudySession session = StudySession.builder()
                .account(account)
                .sessionType("PRACTICE")
                .startedAt(Instant.now())
                .build();

        session = studySessionRepository.save(session);
        return PracticeSessionResponse.fromEntity(session);
    }

    @Transactional
    public PracticeSessionResponse endSession(String email, String sessionId) {
        StudySession session = studySessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Session not found"));

        if (!session.getAccount().getEmail().equals(email)) {
            throw new ApiException("FORBIDDEN", "Session does not belong to this account");
        }

        session.setEndedAt(Instant.now());
        session = studySessionRepository.save(session);
        return PracticeSessionResponse.fromEntity(session);
    }

    @Transactional
    public AttemptResponse submitAttempt(String email, AttemptRequest request) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        StudySession session = null;
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            session = studySessionRepository.findById(UUID.fromString(request.getSessionId()))
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Session not found"));
        }

        ContentItem challenge = contentItemRepository.findById(UUID.fromString(request.getChallengeId()))
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Challenge not found"));

        double randomScoreDouble = 50 + (Math.random() * 50);
        BigDecimal scoreOverall = BigDecimal.valueOf(randomScoreDouble);
        boolean isPassed = randomScoreDouble >= 80.0;
        int latencyMs = (int) (Math.random() * 500) + 100;

        // Build phoneme feedback từ metadata của challenge
        List<PhonemeFeedbackDetail> feedbackList = new ArrayList<>();
        try {
            if (challenge.getMetadataJson() != null) {
                Map<?, ?> meta = objectMapper.readValue(challenge.getMetadataJson(), Map.class);
                String focusPhonemes = (String) meta.get("focus_phonemes");
                if (focusPhonemes != null && !focusPhonemes.isEmpty()) {
                    String[] phonemes = focusPhonemes.split(",");
                    int order = 1;
                    for (String p : phonemes) {
                        feedbackList.add(PhonemeFeedbackDetail.builder()
                                .sequenceOrder(order)
                                .phonemeIpa(p.trim())
                                .score(BigDecimal.valueOf(60 + (Math.random() * 40)))
                                .startTimeMs((order - 1) * 500)
                                .endTimeMs(order * 500)
                                .build());
                        order++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse challenge metadata for phoneme feedback", e);
        }

        // Lưu toàn bộ attempt context vào attemptMetadataJson
        String attemptMetadataJson = null;
        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("audioUrl", request.getAudioUrl());
            meta.put("latencyMs", latencyMs);
            if (!feedbackList.isEmpty()) {
                meta.put("phonemeFeedback", feedbackList);
            }
            attemptMetadataJson = objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            log.error("Failed to serialize attempt metadata", e);
        }

        SessionDetail detail = SessionDetail.builder()
                .session(session)
                .contentItem(challenge)
                .isPassed(isPassed)
                .scoreOverall(scoreOverall)
                .attemptMetadataJson(attemptMetadataJson)
                .build();

        detail = sessionDetailRepository.save(detail);

        if (isPassed) {
            account.setTotalStars(account.getTotalStars() + 3);
            account.setTotalExperience(account.getTotalExperience() + 10);
            accountRepository.save(account);
        }

        AttemptResponse response = AttemptResponse.fromEntity(detail);
        response.setFeedback(
                feedbackList.stream().map(PhonemeFeedbackResponse::fromDetail).collect(Collectors.toList()));

        return response;
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> getAttemptHistory(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        return sessionDetailRepository.findByAccountIdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(AttemptResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
