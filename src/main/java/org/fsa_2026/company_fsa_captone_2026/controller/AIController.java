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
        Boolean consentGivenValue = request.get("consentGiven") instanceof Boolean b ? b : null;
        boolean consentGiven = consentGivenValue != null && consentGivenValue;
        Long asrProcessingTimeMs = extractLong(request.get("asrProcessingTimeMs"));
        if (asrProcessingTimeMs == null) {
            asrProcessingTimeMs = extractLong(request.get("asr_processing_time_ms"));
        }
        if (asrProcessingTimeMs == null) {
            asrProcessingTimeMs = extractLong(request.get("asrProcesingTimeMs"));
        }
        if (asrProcessingTimeMs == null) {
            asrProcessingTimeMs = extractLong(request.get("asrProcessingTime"));
        }
        if (asrProcessingTimeMs == null) {
            asrProcessingTimeMs = extractLong(request.get("asrLatencyMs"));
        }

        if (transcribedText == null || targetText == null) {
            throw new ApiException("BAD_REQUEST", "Thiếu transcribedText hoặc targetText");
        }

        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new java.util.HashMap<>(aiService.provideFeedback(transcribedText, targetText));
        long endTime = System.currentTimeMillis();
        long processingTimeMs = endTime - startTime;

        String aiFeedback = null;
        Object feedbackObj = result.get("feedback");
        if (feedbackObj == null) {
            feedbackObj = result.get("suggestion");
        }
        if (feedbackObj == null) {
            feedbackObj = result.get("errorDetail");
        }
        if (feedbackObj != null) {
            aiFeedback = feedbackObj.toString();
        }

        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("processingTimeMs", processingTimeMs);
        result.put("feedback", aiFeedback);
        result.put("geminiFeedback", aiFeedback);
        result.put("aiScore", result.get("accuracy"));
        result.put("score", result.get("accuracy"));
        result.put("suggestion", aiFeedback);
        result.put("errorDetail", aiFeedback);
        result.put("transcribedText", transcribedText);

        if (consentGiven && authentication != null) {
            int score = 0;
            Object scoreObj = result.get("accuracy");
            if (scoreObj instanceof Number number) {
                score = number.intValue();
            }

            boolean isCorrect = false;
            Object isCorrectObj = result.get("isCorrect");
            if (isCorrectObj instanceof Boolean bool) {
                isCorrect = bool;
            }

            speakingAttemptService.saveAttemptAsync(
                    authentication.getName(),
                    challengeId,
                    targetText,
                    transcribedText,
                    audioUrl,
                    score,
                    isCorrect,
                    dialect,
                    processingTimeMs,
                    asrProcessingTimeMs,
                    aiFeedback
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

    private Long extractLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
