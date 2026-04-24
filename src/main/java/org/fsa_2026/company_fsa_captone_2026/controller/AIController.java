package org.fsa_2026.company_fsa_captone_2026.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.service.AIService;
import org.fsa_2026.company_fsa_captone_2026.service.SpeakingAttemptService;
import org.fsa_2026.company_fsa_captone_2026.service.TTSService;
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
    private final TTSService ttsService;

    @PostMapping("/tts")
    public Map<String, Object> tts(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String voice = request.get("voice");
        if (text == null || text.isBlank()) {
            throw new ApiException("BAD_REQUEST", "Thiếu văn bản để tổng hợp giọng nói");
        }
        return ttsService.synthesize(text, voice);
    }

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
     * Now integrates with SpeakingAttemptService to save data for dataset
     * collection.
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
        result.put("groqFeedback", aiFeedback);
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
                    aiFeedback);
        }

        return result;
    }

    /**
     * Chat đơn giản với Groq.
     * Request body:
     * {
     * "message": "Xin chào Groq"
     * }
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        if (message == null || message.isBlank()) {
            throw new ApiException("BAD_REQUEST", "Thiếu message");
        }
        return aiService.chatWithGroq(message);
    }

    @PostMapping("/explain-quiz-answer")
    public Map<String, Object> explainQuizAnswer(@RequestBody Map<String, Object> request) {
        log.info("[explainQuizAnswer] New request received: {}", request);
        String question = (String) request.get("question");
        String selectedAnswer = (String) request.get("selectedAnswer");
        String correctAnswer = (String) request.get("correctAnswer");
        String skillType = (String) request.get("skillType");
        String transcript = (String) request.get("transcript");
        String correctSentence = (String) request.get("correctSentence");

        if (question == null || selectedAnswer == null || correctAnswer == null) {
            throw new ApiException("BAD_REQUEST", "Thiếu thông tin câu hỏi hoặc đáp án");
        }

        return aiService.explainQuizAnswer(question, selectedAnswer, correctAnswer, skillType, transcript,
                correctSentence);
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
