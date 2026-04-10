package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    // 2026 Stable Models (Discovered via API list)
    private static final String GEMINI_2_5_FLASH_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=";
    private static final String GEMINI_2_0_FLASH_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=";
    private static final String GEMINI_1_5_FLASH_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=";

    public Map<String, Object> evaluatePronunciation(byte[] audioData, String targetText) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new ApiException("CONFIG_ERROR", "Gemini API Key chưa được cấu hình. Vui lòng kiểm tra file application.properties.");
        }

        try {
            String base64Audio = Base64.getEncoder().encodeToString(audioData);

            // Construct Gemini request payload
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();

            // Part 1: Text Prompt
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", String.format(
                "Bạn là chuyên gia ngữ âm học Việt Nam. " +
                "Nhiệm vụ: Đánh giá đoạn âm thanh đính kèm so với câu mẫu: \"%s\". " +
                "Hãy phân tích kỹ các lỗi về dấu thanh, phụ âm đầu (tr/ch, s/x, n/l) và nguyên âm. " +
                "Trả về kết quả duy nhất dưới định dạng JSON như sau: " +
                "{ \"score\": number, \"isCorrect\": boolean, \"transcription\": \"văn bản nghe được\", \"errorDetail\": \"mô tả lỗi\", \"suggestion\": \"hướng dẫn sửa\" }. " +
                "Nếu phát âm đúng > 80%% thì isCorrect là true.", targetText));
            parts.add(textPart);

            // Part 2: Audio Data
            Map<String, Object> audioPart = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mimeType", "audio/wav");
            inlineData.put("data", base64Audio);
            audioPart.put("inlineData", inlineData);
            parts.add(audioPart);

            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);

            return callGeminiWithFallback(requestBody, targetText, null);

        } catch (Exception e) {
            log.error("Gemini Audio Evaluation Error: ", e);
            throw new ApiException("INTERNAL_SERVER_ERROR", "Lỗi xử lý đánh giá phát âm qua audio: " + e.getMessage());
        }
    }

    public Map<String, Object> chatWithGeminiFlash(String message) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new ApiException("CONFIG_ERROR", "Gemini API Key chưa được cấu hình. Vui lòng kiểm tra biến môi trường.");
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", message);
            parts.add(textPart);

            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);

            // Use the new 2026 standard model for chat as well
            Map<String, Object> raw = callGemini(GEMINI_2_5_FLASH_URL, requestBody);
            if (raw == null) {
                return Map.of("reply", "Không nhận được phản hồi từ Gemini.");
            }

            if (raw.get("reply") != null) {
                return raw;
            }

            // Nếu model trả JSON khác format chat, trả text fallback
            return Map.of("reply", raw.toString());

        } catch (Exception e) {
            log.error("Gemini Chat Error: ", e);
            // Fallback to 1.5-flash if 2.5-flash fails in chat
            try {
                log.info("Chat Fallback: Attempting Gemini 1.5 Flash...");
                Map<String, Object> requestBody = new HashMap<>();
                List<Map<String, Object>> contents = new ArrayList<>();
                Map<String, Object> content = new HashMap<>();
                List<Map<String, Object>> parts = new ArrayList<>();
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("text", message);
                parts.add(textPart);
                content.put("parts", parts);
                contents.add(content);
                requestBody.put("contents", contents);

                return callGemini(GEMINI_1_5_FLASH_URL, requestBody);
            } catch (Exception e2) {
                throw new ApiException("AI_ERROR", "Không thể gọi Gemini chat: " + e.getMessage());
            }
        }
    }

    public Map<String, Object> provideFeedback(String transcribedText, String targetText) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new ApiException("CONFIG_ERROR", "Gemini API Key chưa được cấu hình. Vui lòng kiểm tra file application.properties.");
        }

        try {
            // Construct Gemini request payload
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();

            // Text Prompt
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", String.format(
                "Bạn là chuyên gia ngữ âm học Việt Nam. " +
                "NHIỆM VỤ: So sánh văn bản nhận diện được từ audio (ASR) với đáp án mẫu. " +
                "Mẫu: \"%s\" " +
                "Văn bản ASR nhận diện được: \"%s\" " +
                "YẾU TỐ QUAN TRỌNG: " +
                "1. Kiểm tra sự khác biệt, đặc biệt là các cặp âm TR/CH, N/L, S/X, D/GI/R. " +
                "2. Nếu văn bản ASR khác mẫu ở các phụ âm đầu này (ví dụ ASR: \"Núa\", mẫu: \"Lúa\"), báo lỗi phát âm sai. " +
                "3. CHẤM ĐIỂM (0-100) và TRẢ VỀ JSON: { \"score\": number, \"isCorrect\": boolean, \"errorDetail\": \"mô tả lỗi dựa trên sự khác biệt\", \"suggestion\": \"cách sửa lỗi\" }", 
                targetText, transcribedText));
            parts.add(textPart);

            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);

            return callGeminiWithFallback(requestBody, targetText, transcribedText);

        } catch (Exception e) {
            log.error("Gemini Text Evaluation Error: ", e);
            // Instead of throwing exception, try string similarity as the absolute last resort
            if (transcribedText != null) {
                return calculateStringSimilarityFallback(transcribedText, targetText, e.getMessage());
            }
            throw new ApiException("INTERNAL_SERVER_ERROR", "Lỗi xử lý phản hồi từ Gemini: " + e.getMessage());
        }
    }

    private Map<String, Object> callGeminiWithFallback(Map<String, Object> requestBody, String targetText, String transcribedText) {
        StringBuilder errorLog = new StringBuilder();
        
        // 1. Try Gemini 2.5 Flash (New 2026 Standard)
        try {
            log.info("Attempting Gemini 2.5 Flash call...");
            return callGemini(GEMINI_2_5_FLASH_URL, requestBody);
        } catch (Exception e) {
            String msg = e instanceof HttpClientErrorException ? ((HttpClientErrorException)e).getResponseBodyAsString() : e.getMessage();
            log.warn("Gemini 2.5 Flash failed: {}", msg);
            errorLog.append("2.5-Flash: ").append(msg).append("; ");

            if (isQuotaExceeded(msg)) {
                log.warn("Quota exceeded on Gemini 2.5 Flash.");
            }
        }

        // 2. Try Gemini 2.0 Flash (Fallback)
        try {
            log.info("Attempting Gemini 2.0 Flash call...");
            return callGemini(GEMINI_2_0_FLASH_URL, requestBody);
        } catch (Exception e) {
            String msg = e instanceof HttpClientErrorException ? ((HttpClientErrorException)e).getResponseBodyAsString() : e.getMessage();
            log.warn("Gemini 2.0 Flash failed: {}", msg);
            errorLog.append("2.0-Flash: ").append(msg).append("; ");

            if (isQuotaExceeded(msg)) {
                log.warn("Quota exceeded on Gemini 2.0 Flash.");
            }
        }

        // 3. Try Gemini 1.5 Flash (Final AI Fallback)
        try {
            log.info("Attempting Gemini 1.5 Flash call...");
            return callGemini(GEMINI_1_5_FLASH_URL, requestBody);
        } catch (Exception e) {
            String msg = e instanceof HttpClientErrorException ? ((HttpClientErrorException)e).getResponseBodyAsString() : e.getMessage();
            log.warn("Gemini 1.5 Flash failed: {}", msg);
            errorLog.append("1.5-Flash: ").append(msg).append("; ");
        }
        

        // All AI models failed
        log.error("All Gemini models failed. Diagnostics: {}", errorLog.toString());
        if (transcribedText != null) {
            return calculateStringSimilarityFallback(transcribedText, targetText, errorLog.toString());
        }
        throw new ApiException("AI_ERROR", "Tất cả các model AI đều không phản hồi: " + errorLog.toString());
    }

    private Map<String, Object> callGemini(String apiUrl, Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1) First attempt: ask Gemini to return JSON directly
        Map<String, Object> bodyWithConfig = new HashMap<>(requestBody);
        Map<String, Object> generationConfig = new HashMap<>();
        // Với endpoint Gemini hiện tại, key hợp lệ là snake_case
        generationConfig.put("response_mime_type", "application/json");
        bodyWithConfig.put("generationConfig", generationConfig);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(bodyWithConfig, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl + geminiApiKey, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseGeminiResponse(response.getBody());
            }
            throw new ApiException("AI_ERROR", "Lỗi status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            String msg = e.getResponseBodyAsString();

            // 2) Compatibility retry: some model versions reject generationConfig fields
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST
                    && msg != null
                    && msg.toLowerCase().contains("invalid json payload")) {

                log.warn("Gemini rejected generationConfig, retrying without generationConfig...");
                HttpEntity<Map<String, Object>> fallbackEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<Map> fallbackResponse = restTemplate.postForEntity(apiUrl + geminiApiKey, fallbackEntity, Map.class);

                if (fallbackResponse.getStatusCode().is2xxSuccessful() && fallbackResponse.getBody() != null) {
                    return parseGeminiResponse(fallbackResponse.getBody());
                }
                throw new ApiException("AI_ERROR", "Lỗi status (retry): " + fallbackResponse.getStatusCode());
            }

            throw e;
        }
    }

    private boolean isQuotaExceeded(String msg) {
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("quota")
                || lower.contains("resource_exhausted")
                || lower.contains("exceeded your current quota")
                || lower.contains("limit: 0")
                || lower.contains("rate limit")
                || lower.contains("429");
    }

    private Map<String, Object> calculateStringSimilarityFallback(String transcribedText, String targetText, String reason) {
        log.info("Using String Similarity Fallback. Reason: {}", reason);
        
        String s1 = transcribedText.toLowerCase().replaceAll("[.,!?;:]", "").trim();
        String s2 = targetText.toLowerCase().replaceAll("[.,!?;:]", "").trim();
        
        if (s1.isEmpty() || s1.equals("lỗi hệ thống")) {
            return Map.of(
                "score", 0,
                "isCorrect", false,
                "errorDetail", "Không nhận định được giọng nói. (Lỗi AI: " + reason + ")",
                "suggestion", "Vui lòng thử lại sau hoặc kiểm tra kết nối."
            );
        }

        double similarity = getLevenshteinSimilarity(s1, s2);
        int score = (int) (similarity * 100);
        boolean isCorrect = score >= 80;

        String errorDetail = isCorrect ? "Phát âm gần đúng." : "Phát âm chưa chính xác.";
        String suggestion = isCorrect ? "Rất tốt, hãy tiếp tục!" : "Hãy nghe lại mẫu và thử lại nhé.";

        return Map.of(
            "score", score,
            "isCorrect", isCorrect,
            "errorDetail", errorDetail + " (Chế độ dự phòng - AI đang bận)",
            "suggestion", suggestion,
            "ai_error_debug", reason // Thêm để debug
        );
    }

    private double getLevenshteinSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        return (maxLength - getLevenshteinDistance(s1, s2)) / (double) maxLength;
    }

    private int getLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    private Map<String, Object> parseGeminiResponse(Map responseBody) {
        try {
            List<Map> candidates = (List<Map>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;

            String text = (String) parts.get(0).get("text");
            if (text == null) return null;

            // Remove markdown code blocks if present
            String normalized = text;
            if (normalized.startsWith("```json")) {
                normalized = normalized.substring(7);
            }
            if (normalized.endsWith("```")) {
                normalized = normalized.substring(0, normalized.length() - 3);
            }
            normalized = normalized.trim();

            // Try parse JSON first
            try {
                return objectMapper.readValue(normalized, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
                // Not JSON => treat as plain chat reply
                return Map.of("reply", normalized);
            }

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: ", e);
            return null;
        }
    }
}

