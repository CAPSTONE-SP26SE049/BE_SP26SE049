package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${gemini.api-key:${GOOGLE_API_KEY:}}")
    private String googleApiKey;

    @Value("${gemini.endpoint:https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent}")
    private String geminiEndpoint;

    @Value("${groq.api-key:}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${groq.fallback-models:llama-3.3-70b-versatile,llama-3.1-8b-instant}")
    private String groqFallbackModels;

    @Value("${groq.endpoint:https://api.groq.com/openai/v1/chat/completions}")
    private String groqEndpoint;

    @Value("${azure.speech.key:}")
    private String azureSpeechKey;

    @Value("${azure.speech.region:}")
    private String azureSpeechRegion;

    private static final String SYSTEM_INSTRUCTION = "Bạn là chuyên gia ngôn ngữ học lâm sàng. So sánh rawText và targetText. "
            +
            "Phân tích lỗi ngọng N/L, S/X, TR/CH, D/R/GI dựa trên cơ chế vật lý của lưỡi và luồng hơi.";

    private static final String JSON_SCHEMA_INSTRUCTION = "IMPORTANT: Trả về JSON thuần túy với fields: isCorrect(boolean), accuracy(number 0-100), errorType, feedback, shapeKey. "
            +
            "accuracy phải là thang điểm 0-100.";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> evaluatePronunciation(byte[] audioData, String targetText) {
        long azureStart = System.currentTimeMillis();
        AzureTranscriptionResult transcriptionResult = transcribeAudio(audioData);
        long azureLatencyMs = System.currentTimeMillis() - azureStart;

        Map<String, Object> feedback = provideFeedback(transcriptionResult.transcript(), targetText);
        feedback.put("azureTranscript", transcriptionResult.transcript());
        feedback.put("azureLatencyMs", azureLatencyMs);
        feedback.put("azureAccuracy", transcriptionResult.pronunciationAccuracy());
        feedback.put("asrProvider", "azure");
        feedback.put("aiProvider", feedback.getOrDefault("aiProvider", "groq"));
        return feedback;
    }

    public Map<String, Object> chatWithGeminiFlash(String message) {
        if (googleApiKey == null || googleApiKey.isBlank()) {
            throw new ApiException("CONFIG_ERROR",
                    "AI API Key chưa được cấu hình. Vui lòng kiểm tra biến môi trường GOOGLE_API_KEY.");
        }
        return chatWithGeminiOrFallback(message);
    }

    public Map<String, Object> provideFeedback(String transcribedText, String targetText) {
        if (transcribedText == null || targetText == null || transcribedText.isBlank() || targetText.isBlank()) {
            return Map.of(
                    "isCorrect", false,
                    "accuracy", 0,
                    "errorType", "missing_input",
                    "feedback", "Vui lòng gửi đầy đủ văn bản nhận diện và văn bản mẫu.",
                    "shapeKey", "missing_input",
                    "score", 0,
                    "suggestion", "Vui lòng gửi đầy đủ văn bản nhận diện và văn bản mẫu.",
                    "errorDetail", "Thiếu dữ liệu đầu vào để chấm điểm.");
        }

        String normalizedTranscribed = normalizeForComparison(transcribedText);
        String normalizedTarget = normalizeForComparison(targetText);
        if (!normalizedTranscribed.isBlank() && normalizedTranscribed.equals(normalizedTarget)) {
            return Map.of(
                    "isCorrect", true,
                    "accuracy", 100,
                    "errorType", "exact_match",
                    "feedback", "Bạn đọc đúng hoàn toàn. Hãy giữ nhịp nói tự nhiên và rõ âm cuối.",
                    "shapeKey", "exact_match",
                    "score", 100,
                    "suggestion", "Bạn đọc đúng hoàn toàn. Hãy giữ nhịp nói tự nhiên và rõ âm cuối.",
                    "errorDetail", "ASR khớp hoàn toàn với câu mẫu.");
        }

        if (googleApiKey == null || googleApiKey.isBlank()) {
            return calculateStringSimilarityFallback(transcribedText, targetText, "GOOGLE_API_KEY chưa được cấu hình");
        }

        String prompt = buildFeedbackPrompt(transcribedText, targetText);
        return callGroqWithLatency(prompt, transcribedText, targetText,
                "Azure Speech đã transcribe xong, gửi sang Groq");
    }

    public AzureTranscriptionResult transcribeAudio(byte[] audioData) {
        if (azureSpeechKey == null || azureSpeechKey.isBlank() || azureSpeechRegion == null
                || azureSpeechRegion.isBlank()) {
            throw new ApiException("CONFIG_ERROR",
                    "Thiếu cấu hình Azure Speech. Vui lòng kiểm tra azure.speech.key và azure.speech.region.");
        }

        long start = System.currentTimeMillis();
        try {
            String accessToken = fetchAzureSpeechToken();
            String url = UriComponentsBuilder
                    .fromUriString("https://" + azureSpeechRegion
                            + ".stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1")
                    .queryParam("language", "vi-VN")
                    .queryParam("format", "detailed")
                    .build(true)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("audio/wav"));
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(accessToken);
            headers.set("Ocp-Apim-Subscription-Key", azureSpeechKey);
            headers.set("Pronunciation-Assessment",
                    "enabled=true;grading-system=HundredMark;granularity=Phoneme;phoneme-alphabet=IPA");

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(audioData, headers),
                    String.class);

            long latencyMs = System.currentTimeMillis() - start;
            return parseAzureTranscription(response.getBody(), latencyMs);
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.error("Azure Speech transcription failed after {} ms", latencyMs, e);
            return new AzureTranscriptionResult("", 0, latencyMs, e.getMessage());
        }
    }

    private AzureTranscriptionResult parseAzureTranscription(String responseBody, long latencyMs) {
        try {
            if (responseBody == null || responseBody.isBlank()) {
                return new AzureTranscriptionResult("", 0, latencyMs, "Empty Azure response");
            }
            JsonNode root = objectMapper.readTree(responseBody);
            String transcript = "";
            JsonNode displayText = root.at("/DisplayText");
            if (!displayText.isMissingNode()) {
                transcript = displayText.asText("");
            }
            if (transcript.isBlank()) {
                JsonNode nbest = root.at("/NBest");
                if (nbest.isArray() && !nbest.isEmpty()) {
                    transcript = nbest.get(0).path("Display").asText("");
                }
            }
            double accuracy = 0;
            JsonNode pronunciation = root.at("/PronunciationAssessment");
            if (!pronunciation.isMissingNode()) {
                accuracy = pronunciation.path("AccuracyScore").asDouble(0);
            } else {
                JsonNode nbest = root.at("/NBest");
                if (nbest.isArray() && !nbest.isEmpty()) {
                    accuracy = nbest.get(0).path("PronunciationAssessment").path("AccuracyScore").asDouble(0);
                }
            }
            return new AzureTranscriptionResult(transcript, accuracy, latencyMs, null);
        } catch (Exception e) {
            return new AzureTranscriptionResult("", 0, latencyMs, e.getMessage());
        }
    }

    private String fetchAzureSpeechToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Ocp-Apim-Subscription-Key", azureSpeechKey);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://" + azureSpeechRegion + ".api.cognitive.microsoft.com/sts/v1.0/issuetoken",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        return response.getBody();
    }

    private Map<String, Object> chatWithGeminiOrFallback(String message) {
        String[] modelChain = { "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-flash-8b" };
        String lastError = "Unknown error";
        for (String modelName : modelChain) {
            try {
                Map<String, Object> raw = callGeminiModel(message, null, null, modelName);
                if (raw != null && !raw.containsKey("ai_error_debug")) {
                    return raw.get("reply") != null ? raw : Map.of("reply", raw.toString());
                }
                if (raw != null && raw.containsKey("ai_error_debug")) {
                    lastError = (String) raw.get("ai_error_debug");
                }
            } catch (Exception e) {
                lastError = e.getMessage();
            }
        }
        throw new ApiException("AI_ERROR", "Không thể gọi AI chat: " + lastError);
    }

    private Map<String, Object> callGroqWithLatency(String prompt, String transcribedText, String targetText,
            String reason) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            return calculateStringSimilarityFallback(transcribedText, targetText,
                    reason == null ? "Groq API key missing" : reason);
        }

        long groqStart = System.currentTimeMillis();
        try {
            Map<String, Object> raw = callGroqModel(prompt);
            long groqLatencyMs = System.currentTimeMillis() - groqStart;
            Map<String, Object> normalized = normalizeFeedbackResponse(raw, transcribedText, targetText, null);
            normalized.put("groqLatencyMs", groqLatencyMs);
            normalized.put("geminiLatencyMs", 0);
            normalized.put("aiProvider", "groq");
            return addDynamicFallbackContext(normalized, transcribedText, targetText);
        } catch (Exception e) {
            long groqLatencyMs = System.currentTimeMillis() - groqStart;
            Map<String, Object> fallback = calculateStringSimilarityFallback(transcribedText, targetText,
                    e.getMessage());
            fallback.put("groqLatencyMs", groqLatencyMs);
            fallback.put("geminiLatencyMs", 0);
            fallback.put("aiProvider", "local_fallback");
            return fallback;
        }
    }

    private String buildFeedbackPrompt(String transcribedText, String targetText) {
        return String.format(
                "System: %s\nRaw: %s\nTarget: %s\n%s",
                SYSTEM_INSTRUCTION,
                transcribedText,
                targetText,
                JSON_SCHEMA_INSTRUCTION);
    }

    private Map<String, Object> callGeminiModel(String userMessage, String targetText, String transcribedText,
            String modelName) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", userMessage)))));
        requestBody.put("generationConfig", Map.of("responseMimeType", "application/json"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String url = (modelName != null)
                ? "https://generativelanguage.googleapis.com/v1/models/" + modelName + ":generateContent?key="
                        + googleApiKey
                : geminiEndpoint + (geminiEndpoint.contains("?") ? "&" : "?") + "key=" + googleApiKey;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers), Map.class);
            return parseAndNormalizeGeminiResponse(response.getBody(), targetText, transcribedText);
        } catch (HttpClientErrorException e) {
            String msg = e.getResponseBodyAsString();
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS || e.getStatusCode().is5xxServerError()
                    || isQuotaExceeded(msg)) {
                Map<String, Object> retryIndicator = new HashMap<>();
                retryIndicator.put("ai_error_debug", "GEMINI_ERROR: " + modelName + " | " + e.getStatusCode());
                retryIndicator.put("httpStatus", e.getStatusCode().value());
                return retryIndicator;
            }
            throw e;
        } catch (HttpServerErrorException e) {
            Map<String, Object> retryIndicator = new HashMap<>();
            retryIndicator.put("ai_error_debug", "GEMINI_ERROR: " + modelName + " | " + e.getStatusCode());
            retryIndicator.put("httpStatus", e.getStatusCode().value());
            return retryIndicator;
        }
    }

    private Map<String, Object> callGroqModel(String prompt) {
        List<String> modelCandidates = new ArrayList<>();
        if (groqModel != null && !groqModel.isBlank()) {
            modelCandidates.add(groqModel.trim());
        }
        if (groqFallbackModels != null && !groqFallbackModels.isBlank()) {
            for (String candidate : groqFallbackModels.split(",")) {
                String normalized = candidate.trim();
                if (!normalized.isBlank() && !modelCandidates.contains(normalized)) {
                    modelCandidates.add(normalized);
                }
            }
        }

        Exception lastError = null;
        for (String modelName : modelCandidates) {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", modelName);
                requestBody.put("messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_INSTRUCTION),
                        Map.of("role", "user", "content", prompt)));
                requestBody.put("temperature", 0.2);
                requestBody.put("response_format", Map.of("type", "json_object"));
                requestBody.put("top_p", 0.95);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);
                headers.setAccept(List.of(MediaType.APPLICATION_JSON));

                ResponseEntity<Map> response = restTemplate.exchange(
                        groqEndpoint,
                        HttpMethod.POST,
                        new HttpEntity<>(requestBody, headers),
                        Map.class);

                return parseAndNormalizeOpenAiStyleResponse(response.getBody(), null, null, true);
            } catch (HttpClientErrorException e) {
                String body = e.getResponseBodyAsString();
                lastError = e;
                if (!isGroqModelDecommissioned(body)) {
                    throw e;
                }
            }
        }

        if (lastError instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new ApiException("AI_ERROR", "Không thể gọi Groq API với các model đã cấu hình.");
    }

    private boolean isGroqModelDecommissioned(String msg) {
        if (msg == null)
            return false;
        String lower = msg.toLowerCase();
        return lower.contains("decommissioned") || lower.contains("no longer supported")
                || lower.contains("model_decommissioned");
    }

    private boolean shouldFallbackToGroq(HttpClientErrorException e) {
        return e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS
                || e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private boolean isQuotaExceeded(String msg) {
        if (msg == null)
            return false;
        String lower = msg.toLowerCase();
        return lower.contains("quota") || lower.contains("rate limit") || lower.contains("too many requests")
                || lower.contains("429") || lower.contains("limit");
    }

    private Map<String, Object> calculateStringSimilarityFallback(String transcribedText, String targetText,
            String reason) {
        String s1 = normalizeForComparison(transcribedText);
        String s2 = normalizeForComparison(targetText);

        if (s1.isEmpty() || s1.equals("lỗi hệ thống") || s2.isEmpty()) {
            return Map.of(
                    "isCorrect", false,
                    "accuracy", 0,
                    "errorType", "fallback_unavailable",
                    "feedback", "Không nhận định được giọng nói. Vui lòng thử lại sau.",
                    "shapeKey", "fallback_unavailable",
                    "score", 0,
                    "suggestion", "Vui lòng thử lại sau hoặc kiểm tra kết nối.",
                    "errorDetail", "Không nhận định được giọng nói. (Lỗi AI: " + reason + ")");
        }

        double similarity = getLevenshteinSimilarity(s1, s2);
        int score = (int) Math.round(similarity * 100);
        boolean isCorrect = score >= 80;

        String errorType = isCorrect ? "near_match" : "pronunciation_mismatch";
        String feedback = isCorrect ? "Phát âm khá đúng, chỉ cần giữ ổn định tốc độ và âm cuối."
                : "Hãy đọc chậm hơn và sửa lại cặp âm dễ nhầm.";

        return Map.of(
                "isCorrect", isCorrect,
                "accuracy", score,
                "errorType", errorType,
                "feedback", feedback,
                "shapeKey", errorType,
                "score", score,
                "suggestion", feedback,
                "errorDetail",
                (isCorrect ? "Phát âm gần đúng." : "Phát âm chưa chính xác.") + " (Chế độ dự phòng - AI đang bận)",
                "ai_error_debug", reason == null ? "unknown" : reason);
    }

    private double getLevenshteinSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0)
            return 1.0;
        return (maxLength - getLevenshteinDistance(s1, s2)) / (double) maxLength;
    }

    private int getLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    private Map<String, Object> normalizeFeedbackResponse(Map<String, Object> raw, String transcribedText,
            String targetText, String reason) {
        if (raw == null) {
            return calculateStringSimilarityFallback(transcribedText, targetText,
                    reason == null ? "empty AI response" : reason);
        }

        String normalizedTranscribed = normalizeForComparison(transcribedText);
        String normalizedTarget = normalizeForComparison(targetText);
        boolean exactMatch = !normalizedTranscribed.isBlank() && normalizedTranscribed.equals(normalizedTarget);

        boolean isCorrect = exactMatch || asBoolean(raw.get("isCorrect"), false);
        int accuracy = normalizeAccuracyValue(raw.get("accuracy"), raw.get("score"),
                exactMatch ? 100 : (isCorrect ? 80 : 0));
        if (exactMatch)
            accuracy = 100;

        String errorType = firstNonBlank(raw.get("errorType"), raw.get("shapeKey"),
                exactMatch ? "exact_match" : (isCorrect ? "near_match" : "pronunciation_mismatch"));
        String feedback = firstNonBlank(raw.get("feedback"), raw.get("suggestion"), raw.get("errorDetail"),
                exactMatch ? "ASR khớp hoàn toàn với câu mẫu."
                        : (isCorrect ? "Phát âm gần đúng." : "Phát âm chưa chính xác."));
        String shapeKey = firstNonBlank(raw.get("shapeKey"), errorType);

        Map<String, Object> result = new HashMap<>();
        result.put("isCorrect", isCorrect);
        result.put("accuracy", accuracy);
        result.put("errorType", errorType);
        result.put("feedback", feedback);
        result.put("shapeKey", shapeKey);
        result.put("score", accuracy);
        result.put("suggestion", feedback);
        result.put("errorDetail", feedback);
        if (reason != null) {
            result.put("ai_error_debug", reason);
        }
        return result;
    }

    private Map<String, Object> parseAndNormalizeGeminiResponse(Map responseBody, String targetText,
            String transcribedText) {
        try {
            if (responseBody == null) {
                return null;
            }
            List<Map> candidates = (List<Map>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            Map content = (Map) candidates.get(0).get("content");
            if (content == null)
                return null;
            List<Map> parts = (List<Map>) content.get("parts");
            if (parts == null || parts.isEmpty())
                return null;
            Object textObj = parts.get(0).get("text");
            if (textObj == null)
                return null;
            String rawResponse = textObj.toString();
            String cleanJson = rawResponse.replaceAll("(?is)```json", "").replaceAll("(?is)```", "").trim();
            JsonNode node = objectMapper.readTree(cleanJson);
            Map<String, Object> parsed = objectMapper.convertValue(node, Map.class);
            if (targetText == null)
                return parsed;
            Map<String, Object> normalizedResult = normalizeFeedbackResponse(parsed, transcribedText, targetText, null);
            return isMeaningfulAiFeedback(normalizedResult, targetText)
                    ? addDynamicFallbackContext(normalizedResult, transcribedText, targetText)
                    : calculateStringSimilarityFallback(transcribedText, targetText,
                            "AI response thiếu chi tiết hoặc lặp lại câu mẫu");
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseAndNormalizeOpenAiStyleResponse(Map responseBody, String targetText,
            String transcribedText, boolean groq) {
        try {
            if (responseBody == null)
                return null;
            List<Map> choices = (List<Map>) responseBody.get("choices");
            if (choices == null || choices.isEmpty())
                return null;
            Map message = (Map) choices.get(0).get("message");
            if (message == null)
                return null;
            Object contentObj = message.get("content");
            if (contentObj == null)
                return null;
            String rawResponse = contentObj.toString();
            String cleanJson = rawResponse.replaceAll("(?is)```json", "").replaceAll("(?is)```", "").trim();
            if (targetText == null) {
                try {
                    JsonNode node = objectMapper.readTree(cleanJson);
                    return objectMapper.convertValue(node, Map.class);
                } catch (Exception ignored) {
                    return Map.of("reply", cleanJson);
                }
            }
            JsonNode node = objectMapper.readTree(cleanJson);
            Map<String, Object> parsed = objectMapper.convertValue(node, Map.class);
            Map<String, Object> normalizedResult = normalizeFeedbackResponse(parsed, transcribedText, targetText, null);
            return isMeaningfulAiFeedback(normalizedResult, targetText)
                    ? addDynamicFallbackContext(normalizedResult, transcribedText, targetText)
                    : calculateStringSimilarityFallback(transcribedText, targetText,
                            groq ? "Groq response thiếu chi tiết hoặc lặp lại câu mẫu"
                                    : "AI response thiếu chi tiết hoặc lặp lại câu mẫu");
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> addDynamicFallbackContext(Map<String, Object> feedback, String transcribedText,
            String targetText) {
        if (feedback == null)
            return null;
        Map<String, Object> result = new HashMap<>(feedback);
        String feedbackText = cleanFeedbackText((String) result.get("feedback"));
        String target = cleanFeedbackText(targetText);
        String transcription = cleanFeedbackText(transcribedText);

        if (!target.isBlank() && feedbackText.equalsIgnoreCase(target)) {
            feedbackText = "Hãy nghe lại mẫu và đọc chậm từng tiếng để sửa đúng âm, đúng dấu và đúng nhịp.";
        }
        if (!transcription.isBlank() && !target.isBlank() && transcription.equalsIgnoreCase(target)) {
            feedbackText = "Kết quả đã tốt, tiếp tục duy trì độ rõ và sự ổn định khi phát âm.";
        }
        if (feedbackText.isBlank()) {
            feedbackText = "Hãy đọc chậm hơn, nghe lại mẫu và sửa lại từng âm, từng dấu thanh một cách rõ ràng.";
        }

        result.put("feedback", stripTrailingSentencePunctuation(feedbackText));
        result.put("suggestion", stripTrailingSentencePunctuation(feedbackText));
        return result;
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value != null) {
                String text = value.toString().trim();
                if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                    return text;
                }
            }
        }
        return "";
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean b)
            return b;
        if (value == null)
            return defaultValue;
        return Boolean.parseBoolean(value.toString());
    }

    private int normalizeAccuracyValue(Object primary, Object secondary, int fallback) {
        int candidate = parseAccuracy(primary, Integer.MIN_VALUE);
        if (candidate == Integer.MIN_VALUE) {
            candidate = parseAccuracy(secondary, fallback);
        }
        if (candidate <= 1) {
            candidate = candidate == 1 ? 100 : fallback;
        }
        return Math.max(0, Math.min(100, candidate));
    }

    private int parseAccuracy(Object value, int fallback) {
        if (value instanceof Number n) {
            return scaleAccuracyNumber(n.doubleValue(), fallback);
        }
        if (value != null) {
            try {
                return scaleAccuracyNumber(Double.parseDouble(value.toString()), fallback);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private int scaleAccuracyNumber(double value, int fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        if (value <= 1.0) {
            return (int) Math.round(value * 100.0);
        }
        return (int) Math.round(value);
    }

    private Number asNumber(Object primary, Number fallback) {
        if (primary instanceof Number n)
            return n;
        if (primary != null) {
            try {
                return Double.parseDouble(primary.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    private String normalizeForComparison(String text) {
        String cleaned = cleanFeedbackText(text);
        if (cleaned.isBlank())
            return "";
        return cleaned.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String cleanFeedbackText(String text) {
        return text == null ? "" : text.trim();
    }

    private String stripTrailingSentencePunctuation(String text) {
        if (text == null)
            return "";
        return text.trim().replaceAll("[\\s]*[\\.\\!\\?。！？]+[\\s]*$", "").trim();
    }

    private boolean isMeaningfulAiFeedback(Map<String, Object> feedback, String targetText) {
        if (feedback == null)
            return false;
        String feedbackText = firstNonBlank(feedback.get("feedback"), feedback.get("suggestion"),
                feedback.get("errorDetail"));
        String normalizedTarget = normalizeForComparison(targetText);
        if (feedbackText.isBlank())
            return false;
        if (!normalizedTarget.isEmpty() && normalizeForComparison(feedbackText).equals(normalizedTarget))
            return false;
        return feedbackText.length() >= 10;
    }

    private Map<String, Object> parseFeedbackFromText(String content) {
        Map<String, Object> result = new HashMap<>();
        result.put("feedback", content);
        result.put("errorType", "unparsed_text");
        result.put("shapeKey", "unparsed_text");
        result.put("isCorrect", false);
        result.put("accuracy", 0);
        return result;
    }

    public record AzureTranscriptionResult(String transcript, double pronunciationAccuracy, long latencyMs,
            String error) {
    }
}
