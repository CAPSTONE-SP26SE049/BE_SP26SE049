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
    private final org.fsa_2026.company_fsa_captone_2026.service.TTSService ttsService;
    private final org.fsa_2026.company_fsa_captone_2026.service.FirebaseStorageService firebaseStorageService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @PostMapping("/tts")
    public Map<String, Object> tts(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String voice = request.get("voice");
        if (text == null || text.isBlank()) {
            throw new ApiException("BAD_REQUEST", "Văn bản không được để trống");
        }
        return ttsService.synthesize(text, voice);
    }



    /**
     * Chấm phát âm trực tiếp: upload audio + câu mẫu, ASR local rồi Groq feedback.
     *
     * @param audio      file multipart — bắt buộc {@code .webm}
     * @param targetText câu tiếng Việt chuẩn cần đối chiếu
     * @return map điểm, feedback, {@code audioUrl} (Firebase), transcript, …
     * @throws IOException khi đọc multipart
     *
     * <p><b>Note:</b> Đã fix BUG-001 — validate {@code .webm} qua
     * {@link org.fsa_2026.company_fsa_captone_2026.service.AIService#evaluatePronunciation(org.springframework.web.multipart.MultipartFile, String)}
     * trước khi gọi Groq (đồng bộ Entry Test).</p>
     */
    @PostMapping("/evaluate-pronunciation")
    public Map<String, Object> evaluatePronunciation(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("targetText") String targetText) throws IOException {

        Map<String, Object> result = new java.util.HashMap<>(aiService.evaluatePronunciation(audio, targetText));

        // Upload to Firebase
        try {
            String audioUrl = firebaseStorageService.uploadFile(audio, "pronunciation-eval");
            result.put("audioUrl", audioUrl);
            log.info("Uploaded pronunciation evaluation audio to: {}", audioUrl);
        } catch (Exception e) {
            log.error("Failed to upload audio to Firebase", e);
        }

        return result;
    }

    /**
     * Feedback sau ASR phía client: so sánh transcript với câu mẫu, tùy chọn lưu attempt.
     *
     * @param request        JSON: {@code transcribedText}, {@code targetText}, {@code audioUrl}, {@code consentGiven}, …
     * @param authentication JWT — dùng khi {@code consentGiven=true} để lưu dataset
     * @return điểm, feedback Groq, metadata thời gian xử lý
     *
     * <p><b>Note:</b> Đã fix BUG-001 — nếu gửi {@code audioUrl} thì phải trỏ file {@code .webm}
     * ({@link org.fsa_2026.company_fsa_captone_2026.service.AIService#validateFeedbackAudioUrl}).</p>
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
        aiService.validateFeedbackAudioUrl(audioUrl);
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

        Integer asrScore = extractInteger(request.get("asrScore"));
        if (asrScore == null) {
            asrScore = extractInteger(request.get("score"));
        }

        Object wordDetailsObj = request.get("wordDetails");
        if (wordDetailsObj == null) {
            wordDetailsObj = request.get("word_details");
        }
        String wordDetailsJson = null;
        if (wordDetailsObj != null) {
            try {
                wordDetailsJson = objectMapper.writeValueAsString(wordDetailsObj);
            } catch (Exception e) {
                log.error("Failed to stringify wordDetails", e);
            }
        }

        String recordId = (String) request.get("recordId");
        if (recordId == null) {
            recordId = (String) request.get("record_id");
        }

        if (transcribedText == null || targetText == null) {
            throw new ApiException("BAD_REQUEST", "Thiếu transcribedText hoặc targetText");
        }

        long startTime = System.currentTimeMillis();
        String focusErrorTag = null;
        if (dialect != null && !dialect.isBlank()) {
            focusErrorTag = aiService.findErrorTagUnitId(dialect);
        }
        Map<String, Object> result = new java.util.HashMap<>(aiService.provideFeedback(transcribedText, targetText, focusErrorTag));
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
        // Hệ thống chấm điểm (ASR) là chuẩn; AI chỉ feedback, không ghi đè điểm.
        int finalScore = (asrScore != null) ? asrScore : extractInteger(result.get("accuracy"));
        boolean finalIsCorrect = finalScore >= 80;
        result.put("aiScore", result.get("accuracy"));     // điểm tham khảo từ AI
        result.put("score", finalScore);                       // điểm chính thức từ ASR
        result.put("suggestion", aiFeedback);
        result.put("errorDetail", aiFeedback);
        result.put("transcribedText", transcribedText);

        result.put("asrScore", asrScore);
        result.put("wordDetails", wordDetailsObj);
        result.put("recordId", recordId);

        if (consentGiven && authentication != null) {
            speakingAttemptService.saveAttemptAsync(
                    authentication.getName(),
                    challengeId,
                    targetText,
                    transcribedText,
                    audioUrl,
                    finalScore,
                    finalIsCorrect,
                    dialect,
                    processingTimeMs,
                    asrProcessingTimeMs,
                    aiFeedback,
                    asrScore,
                    wordDetailsJson,
                    recordId);
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
        Boolean isCorrect = (Boolean) request.get("isCorrect");

        if (question == null || selectedAnswer == null || correctAnswer == null) {
            throw new ApiException("BAD_REQUEST", "Thiếu thông tin câu hỏi hoặc đáp án");
        }

        return aiService.explainQuizAnswer(question, selectedAnswer, correctAnswer, skillType, transcript,
                correctSentence, isCorrect);
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

    private Integer extractInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
