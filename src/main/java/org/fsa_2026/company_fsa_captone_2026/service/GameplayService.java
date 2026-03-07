package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameplayService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptPhonemeFeedbackRepository feedbackRepository;
    private final ChallengeRepository challengeRepository;

    @Transactional
    public PracticeSessionResponse startSession(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        PracticeSession session = PracticeSession.builder()
                .account(account)
                .startedAt(Instant.now())
                .build();

        session = practiceSessionRepository.save(session);
        return PracticeSessionResponse.fromEntity(session);
    }

    @Transactional
    public PracticeSessionResponse endSession(String email, String sessionId) {
        PracticeSession session = practiceSessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Session not found"));

        if (!session.getAccount().getEmail().equals(email)) {
            throw new ApiException("FORBIDDEN", "Session does not belong to this account");
        }

        session.setEndedAt(Instant.now());
        session = practiceSessionRepository.save(session);

        return PracticeSessionResponse.fromEntity(session);
    }

    @Transactional
    public AttemptResponse submitAttempt(String email, AttemptRequest request) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        PracticeSession session = practiceSessionRepository.findById(UUID.fromString(request.getSessionId()))
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Session not found"));

        Challenge challenge = challengeRepository.findById(UUID.fromString(request.getChallengeId()))
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Challenge not found"));

        // Simulate Gameplay Logic/AI Scoring
        // In reality, this would call a Python ML endpoint via RestTemplate/WebClient
        double randomScoreDouble = 50 + (Math.random() * 50); // Random score between 50 and 100
        BigDecimal scoreOverall = BigDecimal.valueOf(randomScoreDouble);
        boolean isPassed = randomScoreDouble >= 80.0;

        Attempt attempt = Attempt.builder()
                .account(account)
                .session(session)
                .challenge(challenge)
                .audioUrl(request.getAudioUrl())
                .scoreOverall(scoreOverall)
                .isPassed(isPassed)
                .latencyMs((int) (Math.random() * 500) + 100)
                .createdAt(Instant.now())
                .build();

        attempt = attemptRepository.save(attempt);

        // Simulate detailed feedback based on focus phonemes
        List<AttemptPhonemeFeedback> feedbackList = new ArrayList<>();
        if (challenge.getFocusPhonemes() != null && !challenge.getFocusPhonemes().isEmpty()) {
            String[] phonemes = challenge.getFocusPhonemes().split(","); // E.g., "tr,ch"
            int order = 1;
            for (String p : phonemes) {
                AttemptPhonemeFeedback feedback = AttemptPhonemeFeedback.builder()
                        .attempt(attempt)
                        .sequenceOrder(order++)
                        .phonemeIpa(p.trim())
                        .score(BigDecimal.valueOf(60 + (Math.random() * 40)))
                        .startTimeMs((order - 1) * 500)
                        .endTimeMs(order * 500)
                        .build();
                feedbackList.add(feedbackRepository.save(feedback));
            }
        }

        // Update User Profile stats if passed
        if (isPassed) {
            UserProfile profile = userProfileRepository.findByAccountId(account.getId()).orElse(null);
            if (profile != null) {
                profile.setTotalStars((profile.getTotalStars() != null ? profile.getTotalStars() : 0) + 3);
                profile.setTotalExperience(
                        (profile.getTotalExperience() != null ? profile.getTotalExperience() : 0) + 10);
                userProfileRepository.save(profile);
            }
        }

        AttemptResponse response = AttemptResponse.fromEntity(attempt);
        response.setFeedback(
                feedbackList.stream().map(PhonemeFeedbackResponse::fromEntity).collect(Collectors.toList()));

        return response;
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> getAttemptHistory(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Account not found"));

        return attemptRepository.findByAccountIdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(AttemptResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
