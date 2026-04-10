package org.fsa_2026.company_fsa_captone_2026.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.service.AIService;
import org.fsa_2026.company_fsa_captone_2026.service.SpeakingAttemptService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final SpeakingAttemptService speakingAttemptService;

    @PostMapping("/evaluate-pronunciation")
    public Map<String, Object> evaluatePronunciation(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("targetText") String targetText) throws IOException {

        if (audio.isEmpty()) {
            throw new ApiException("BAD_REQUEST", "File âm thanh không được để trống");
        }

        return aiService.evaluatePronunciation(audio.getBytes(), targetText);
    }

    /**
     * AI Feedback endpoint for text comparison (ASR based).
     * Now integrates with SpeakingAttemptService to save data for dataset collection.
     */
    @PostMapping("/feedback")
    public Map<String, Object> getFeedback(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String transcribedText = (String) request.get("transcribedText");
        String targetText = (String) request.get("targetText");
        String challengeId = (String) request.get("challengeId");
        String dialect = (String) request.get("dialect");
        String audioUrl = (String) request.get("audioUrl");
        Boolean consentGivenValue = (Boolean) request.get("consentGiven");
        boolean consentGiven = consentGivenValue != null && consentGivenValue;

        if (transcribedText == null || targetText == null) {
            throw new ApiException("BAD_REQUEST", "Thiếu transcribedText hoặc targetText");
        }

        // 1. Get AI Analysis
        Map<String, Object> result = aiService.provideFeedback(transcribedText, targetText);

        // 2. Async save if consent given
        if (consentGiven && authentication != null) {
            Object scoreObj = result.get("score");
            int score = 0;
            if (scoreObj instanceof Number) {
                score = ((Number) scoreObj).intValue();
            }
            
            boolean isCorrect = false;
            Object isCorrectObj = result.get("isCorrect");
            if (isCorrectObj instanceof Boolean) {
                isCorrect = (Boolean) isCorrectObj;
            }

            speakingAttemptService.saveAttemptAsync(
                    authentication.getName(),
                    challengeId,
                    targetText,
                    transcribedText,
                    audioUrl,
                    score,
                    isCorrect,
                    dialect
            );
        }

        return result;
    }

    /**
     * Chat đơn giản với Gemini Flash.
     * Request body:
     * {
     *   "message": "Xin chào Gemini"
     * }
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        if (message == null || message.isBlank()) {
            throw new ApiException("BAD_REQUEST", "Thiếu message");
        }
        return aiService.chatWithGeminiFlash(message);
    }
}
