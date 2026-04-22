package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${groq.api-key:}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${groq.fallback-models:llama-3.3-70b-versatile,llama-3.1-8b-instant}")
    private String groqFallbackModels;

    @Value("${groq.endpoint:https://api.groq.com/openai/v1/chat/completions}")
    private String groqEndpoint;

    @Value("${asr.local.endpoint:http://localhost:8000/asr}")
    private String localAsrEndpoint;

    public static final String SYSTEM_INSTRUCTION = "Bạn là chuyên gia phân tích phát âm tiếng Việt. So sánh rawText với targetText và trả về nhận xét sư phạm, cụ thể, hữu ích. Ưu tiên phát hiện các lỗi vùng miền và lỗi phát âm phổ biến như N/L, S/X, TR/CH, D/R/GI, âm cuối, dấu thanh, nguyên âm và phụ âm đầu. Không được trả lời chung chung, không được lặp lại nguyên văn targetText, và không được dùng câu ngắn kiểu 'Phát âm chưa chính xác' nếu chưa giải thích vì sao.";
    public static final String JSON_SCHEMA_INSTRUCTION = "Trả về JSON thuần túy với các fields: accuracy (0-100), detectedError (mô tả lỗi cụ thể), feedback (ít nhất 2 câu, nêu lỗi và cách sửa), suggestion (gợi ý ngắn gọn), errorDetail (diễn giải chi tiết hơn feedback), isRegional (boolean), isCorrect (boolean), shapeKey (exact_match, near_match, pronunciation_mismatch, regional_error, missing_input).";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> evaluatePronunciation(byte[] audioData, String targetText) {
        long asrStart = System.currentTimeMillis();
        AzureTranscriptionResult transcriptionResult = transcribeWithLocalAsr(audioData);
        long asrLatencyMs = System.currentTimeMillis() - asrStart;

        Map<String, Object> feedback = provideFeedback(transcriptionResult.transcript(), targetText);
        feedback.put("azureTranscript", transcriptionResult.transcript());
        feedback.put("azureLatencyMs", asrLatencyMs);
        feedback.put("azureAccuracy", transcriptionResult.pronunciationAccuracy());
        feedback.put("asrProvider", transcriptionResult.provider() != null ? transcriptionResult.provider() : "none");
        feedback.put("aiProvider", feedback.getOrDefault("aiProvider", "groq"));
        return feedback;
    }

    private AzureTranscriptionResult transcribeWithLocalAsr(byte[] audioData) {
        long start = System.currentTimeMillis();
        try {
            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("audio", new org.springframework.core.io.ByteArrayResource(audioData) {
                @Override
                public String getFilename() {
                    return "recording.webm";
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<Map> response = restTemplate.postForEntity(localAsrEndpoint, new HttpEntity<>(body, headers), Map.class);
            long latencyMs = System.currentTimeMillis() - start;
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String text = (String) response.getBody().get("text");
                return new AzureTranscriptionResult(text != null ? text : "", 0, latencyMs, "local_asr", null);
            }
            return new AzureTranscriptionResult("", 0, latencyMs, "local_asr", "Local ASR status: " + response.getStatusCode());
        } catch (Exception e) {
            log.warn("Local ASR failed, fallback to secondary if available: {}", e.getMessage());
            return new AzureTranscriptionResult("", 0, System.currentTimeMillis() - start, "local_asr", e.getMessage());
        }
    }

    public Map<String, Object> chatWithGroq(String message) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new ApiException("CONFIG_ERROR", "Groq API Key chưa được cấu hình. Vui lòng kiểm tra biến môi trường GROQ_API_KEY.");
        }
        return chatWithGroqOrFallback(message);
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
                    "errorDetail", "Thiếu dữ liệu đầu vào để chấm điểm.",
                    "aiProvider", "groq");
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
                    "errorDetail", "ASR khớp hoàn toàn với câu mẫu.",
                    "aiProvider", "groq");
        }

        String prompt = buildFeedbackPrompt(transcribedText, targetText);
        return callGroqFeedbackWithLatency(prompt, transcribedText, targetText);
    }

    private Map<String, Object> callGroqFeedbackWithLatency(String prompt, String transcribedText, String targetText) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> raw = callGroqModel(prompt);
            long latency = System.currentTimeMillis() - start;
            Map<String, Object> normalized = normalizeFeedbackResponse(raw, transcribedText, targetText, null);
            normalized.put("groqLatencyMs", latency);
            normalized.put("aiProvider", "groq");
            return normalized;
        } catch (Exception e) {
            log.error("Groq feedback failed", e);
            throw new ApiException("AI_ERROR", "Không thể gọi Groq AI Feedback: " + e.getMessage());
        }
    }

    private Map<String, Object> chatWithGroqOrFallback(String message) {
        String lastError = "Unknown error";
        for (String modelName : groqModelCandidates()) {
            try {
                Map<String, Object> raw = callGroqModel(message, modelName);
                if (raw != null) return raw;
            } catch (Exception e) {
                lastError = e.getMessage();
            }
        }
        throw new ApiException("AI_ERROR", "Không thể gọi Groq API: " + lastError);
    }

    private List<String> groqModelCandidates() {
        List<String> candidates = new ArrayList<>();
        if (groqModel != null && !groqModel.isBlank()) candidates.add(groqModel.trim());
        if (groqFallbackModels != null && !groqFallbackModels.isBlank()) {
            for (String candidate : groqFallbackModels.split(",")) {
                String normalized = candidate.trim();
                if (!normalized.isBlank() && !candidates.contains(normalized)) candidates.add(normalized);
            }
        }
        return candidates;
    }

    private String buildFeedbackPrompt(String transcribedText, String targetText) {
        return String.format(
                "System instruction:\n%s\n\n" +
                        "Input:\n" +
                        "- rawText: %s\n" +
                        "- targetText: %s\n\n" +
                        "Output requirements:\n%s\n\n" +
                        "Rules:\n" +
                        "1. Return JSON only.\n" +
                        "2. feedback must be specific, natural, and actionable.\n" +
                        "3. Do not say generic sentences like 'Phát âm chưa chính xác.' unless you also explain why.\n" +
                        "4. If pronunciation is nearly correct, say what is close and what still needs fixing.\n" +
                        "5. If there is a regional pronunciation issue, mention the likely sound pair and mouth/tongue/articulation clue.",
                SYSTEM_INSTRUCTION,
                transcribedText,
                targetText,
                JSON_SCHEMA_INSTRUCTION);
    }

    private Map<String, Object> callGroqModel(String userMessage, String modelName) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new ApiException("CONFIG_ERROR", "Groq API Key chưa được cấu hình. Vui lòng kiểm tra biến môi trường GROQ_API_KEY.");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_INSTRUCTION),
                Map.of("role", "user", "content", userMessage)));
        requestBody.put("temperature", 0.2);
        requestBody.put("response_format", Map.of("type", "json_object"));
        requestBody.put("top_p", 0.95);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> response = restTemplate.exchange(groqEndpoint, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class);
        return parseAndNormalizeOpenAiStyleResponse(response.getBody(), null, null, true);
    }

    private Map<String, Object> callGroqModel(String prompt) {
        Exception lastError = null;
        for (String modelName : groqModelCandidates()) {
            try {
                return callGroqModel(prompt, modelName);
            } catch (Exception e) {
                lastError = e;
            }
        }
        if (lastError instanceof RuntimeException runtimeException) throw runtimeException;
        throw new ApiException("AI_ERROR", "Không thể gọi Groq API với các model đã cấu hình.");
    }

    private String normalizeForComparison(String text) {
        if (text == null) return "";
        return text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private Map<String, Object> normalizeFeedbackResponse(Map<String, Object> raw, String transcribedText, String targetText, String reason) {
        if (raw == null) {
            throw new ApiException("AI_ERROR", "Groq trả về phản hồi rỗng.");
        }
        String normalizedTranscribed = normalizeForComparison(transcribedText);
        String normalizedTarget = normalizeForComparison(targetText);
        boolean exactMatch = !normalizedTranscribed.isBlank() && normalizedTranscribed.equals(normalizedTarget);

        boolean isCorrect = exactMatch || asBoolean(raw.get("isCorrect"), false);
        int accuracy = normalizeAccuracyValue(raw.get("accuracy"), raw.get("score"), exactMatch ? 100 : (isCorrect ? 80 : 0));
        if (exactMatch) accuracy = 100;

        String errorType = firstNonBlank(raw.get("errorType"), raw.get("shapeKey"), exactMatch ? "exact_match" : (isCorrect ? "near_match" : "pronunciation_mismatch"));
        String feedback = firstNonBlank(raw.get("feedback"), raw.get("suggestion"), raw.get("errorDetail"), raw.get("detectedError"), exactMatch ? "ASR khớp hoàn toàn với câu mẫu." : "");
        if (feedback.isBlank()) {
            feedback = exactMatch ? "ASR khớp hoàn toàn với câu mẫu." : "Groq không trả về nội dung phản hồi phù hợp.";
        }
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
        result.put("isRegional", asBoolean(raw.get("isRegional"), false));
        result.put("aiProvider", "groq");
        if (reason != null) result.put("ai_error_debug", reason);
        return result;
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }

    private int normalizeAccuracyValue(Object accuracyValue, Object scoreValue, int defaultValue) {
        Object value = accuracyValue != null ? accuracyValue : scoreValue;
        if (value instanceof Number number) return Math.max(0, Math.min(100, number.intValue()));
        if (value instanceof String text) {
            try { return Math.max(0, Math.min(100, Integer.parseInt(text.trim()))); } catch (NumberFormatException ignored) { }
        }
        return defaultValue;
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value != null) {
                String text = value.toString().trim();
                if (!text.isBlank()) return text;
            }
        }
        return "";
    }

    private Map<String, Object> parseAndNormalizeOpenAiStyleResponse(Map responseBody, String targetText, String transcribedText, boolean groq) {
        try {
            if (responseBody == null) return null;
            List<Map> choices = (List<Map>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map message = (Map) choices.get(0).get("message");
            if (message == null) return null;
            Object contentObj = message.get("content");
            if (contentObj == null) return null;
            String rawResponse = contentObj.toString();
            String cleanJson = rawResponse.replaceAll("(?is)```json", "").replaceAll("(?is)```", "").trim();
            if (targetText == null) {
                try {
                    JsonNode node = objectMapper.readTree(cleanJson);
                    return objectMapper.convertValue(node, Map.class);
                } catch (Exception ignored) {
                    return Map.of("reply", cleanJson, "aiProvider", "groq");
                }
            }
            JsonNode node = objectMapper.readTree(cleanJson);
            Map<String, Object> parsed = objectMapper.convertValue(node, Map.class);
            return normalizeFeedbackResponse(parsed, transcribedText, targetText, null);
        } catch (Exception e) {
            return null;
        }
    }

    public record AzureTranscriptionResult(String transcript, double pronunciationAccuracy, long latencyMs, String provider, String error) {}
}
