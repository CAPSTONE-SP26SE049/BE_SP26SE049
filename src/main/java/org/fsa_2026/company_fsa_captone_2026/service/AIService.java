package org.fsa_2026.company_fsa_captone_2026.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.WebmAudioValidator;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;

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

    @Value("${asr.local.endpoint:https://nguyenductuan-speak-journey-vn.hf.space/api/v1/transcribe}")
    private String localAsrEndpoint;

    @Value("${asr.local.model:}")
    private String localAsrModel;

    @Value("${asr.local.language:vi}")
    private String localAsrLanguage;

    @Value("${asr.local.sampling-rate:16000}")
    private int localAsrSamplingRate;

    public static final String SYSTEM_INSTRUCTION = "Bạn là chuyên gia phân tích phát âm tiếng Việt. So sánh rawText với targetText và trả về nhận xét sư phạm, cụ thể, hữu ích. Hệ thống hiện tại hỗ trợ chẩn đoán và phân tích các lỗi phát âm/vùng miền sau:\n{availableErrorTags}\nHãy ưu tiên đối chiếu phát hiện lỗi xem người học có mắc phải lỗi nào trong danh sách trên hay không. Không được trả lời chung chung, không được lặp lại nguyên văn targetText, và không được dùng câu ngắn kiểu 'Phát âm chưa chính xác' nếu chưa giải thích vì sao.";
    public static final String JSON_SCHEMA_INSTRUCTION = "Trả về JSON thuần túy với các fields: accuracy (0-100), detectedError (mô tả lỗi cụ thể), feedback (ít nhất 2 câu, nêu lỗi và cách sửa), suggestion (gợi ý ngắn gọn), errorDetail (diễn giải chi tiết hơn feedback), isRegional (boolean), isCorrect (boolean), shapeKey (exact_match, near_match, pronunciation_mismatch, regional_error, missing_input).";

    private final ObjectMapper objectMapper;
    private final LearningUnitRepository learningUnitRepository;
    private final SystemConfigService systemConfigService;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Chấm phát âm từ multipart — validate {@code .webm} trước khi gọi ASR + Groq.
     *
     * @param audio      file âm thanh upload
     * @param targetText câu mẫu chuẩn tiếng Việt
     * @return kết quả ASR + AI (accuracy, feedback, azureTranscript, …)
     * @throws java.io.IOException khi đọc bytes từ multipart
     *
     * <p><b>Note:</b> Đã fix BUG-001 — dùng {@link WebmAudioValidator#validateMultipart} đồng bộ với Entry Test
     * trước khi gửi byte[] vào pipeline Groq.</p>
     */
    public Map<String, Object> evaluatePronunciation(MultipartFile audio, String targetText) throws java.io.IOException {
        return evaluatePronunciation(audio, targetText, null);
    }

    /**
     * Chấm phát âm multipart có focus tag miền — validate .webm trước pipeline ASR (Daily Challenge, Entry Test).
     */
    public Map<String, Object> evaluatePronunciation(MultipartFile audio, String targetText, String focusErrorTag)
            throws java.io.IOException {
        WebmAudioValidator.validateMultipart(audio);
        return evaluatePronunciation(audio.getBytes(), targetText, focusErrorTag);
    }

    /**
     * Chấm phát âm từ byte[] (caller đã validate multipart nếu nguồn là upload).
     *
     * @param audioData  nội dung file âm thanh
     * @param targetText câu mẫu
     * @return kết quả chấm điểm
     */
    public Map<String, Object> evaluatePronunciation(byte[] audioData, String targetText) {
        return evaluatePronunciation(audioData, targetText, null);
    }

    /**
     * Pipeline ASR (local) + Groq feedback, có thể focus theo error tag miền (N/L, …).
     *
     * @param audioData      bytes âm thanh
     * @param targetText     câu mẫu
     * @param focusErrorTag  mã tag lỗi (nullable) từ dialect / region category
     * @return map kết quả chấm điểm
     */
    public Map<String, Object> evaluatePronunciation(byte[] audioData, String targetText, String focusErrorTag) {
        long asrStart = System.currentTimeMillis();
        AzureTranscriptionResult transcriptionResult = transcribeWithLocalAsr(audioData, targetText);
        long asrLatencyMs = System.currentTimeMillis() - asrStart;

        Map<String, Object> feedback = provideFeedback(transcriptionResult.transcript(), targetText, focusErrorTag);
        feedback.put("azureTranscript", transcriptionResult.transcript());
        feedback.put("azureLatencyMs", asrLatencyMs);
        feedback.put("azureAccuracy", transcriptionResult.pronunciationAccuracy());
        feedback.put("asrProvider", transcriptionResult.provider() != null ? transcriptionResult.provider() : "none");
        feedback.put("aiProvider", feedback.getOrDefault("aiProvider", "groq"));

        // Populate word_details by aligning targetText and transcribedText if not
        // provided by ASR
        List<Map<String, Object>> wordDetails = transcriptionResult.wordDetails();
        String transcript = transcriptionResult.transcript() != null ? transcriptionResult.transcript() : "";

        if (wordDetails == null || wordDetails.isEmpty()) {
            wordDetails = generateWordDetails(targetText, transcript);
        }
        feedback.put("word_details", wordDetails);
        return feedback;
    }

    /**
     * Simple word alignment between target and transcribed text for Vietnamese.
     */
    private List<Map<String, Object>> generateWordDetails(String target, String transcribed) {
        if (target == null)
            return Collections.emptyList();

        String[] targetWords = target.trim().split("\\s+");
        String[] transcribedWords = (transcribed != null ? transcribed.trim() : "").split("\\s+");

        List<Map<String, Object>> details = new ArrayList<>();

        // Simple alignment: compare words in order
        // For a more robust solution, use Levenshtein distance or Needleman-Wunsch
        int tIdx = 0;
        int rIdx = 0;

        while (tIdx < targetWords.length) {
            String tWord = targetWords[tIdx].toLowerCase().replaceAll("[^\\p{L}]", "");
            Map<String, Object> wordMap = new HashMap<>();
            wordMap.put("word", targetWords[tIdx]);

            boolean found = false;
            // Look ahead a bit to find a match if there's a skip
            for (int i = 0; i < Math.min(3, transcribedWords.length - rIdx); i++) {
                String rWord = transcribedWords[rIdx + i].toLowerCase().replaceAll("[^\\p{L}]", "");
                if (tWord.equals(rWord)) {
                    wordMap.put("status", "correct");
                    wordMap.put("score", 100);
                    rIdx += (i + 1);
                    found = true;
                    break;
                } else if (isNearMatch(tWord, rWord)) {
                    wordMap.put("status", "near");
                    wordMap.put("score", 70);
                    rIdx += (i + 1);
                    found = true;
                    break;
                }
            }

            if (!found) {
                wordMap.put("status", "wrong");
                wordMap.put("score", 0);
            }

            details.add(wordMap);
            tIdx++;
        }

        return details;
    }

    private boolean isNearMatch(String w1, String w2) {
        if (w1.length() < 2 || w2.length() < 2)
            return false;
        // Simple heuristic: if first letter is same and length difference is small
        return w1.charAt(0) == w2.charAt(0) && Math.abs(w1.length() - w2.length()) <= 1;
    }

    private AzureTranscriptionResult transcribeWithLocalAsr(byte[] audioData, String targetText) {
        long start = System.currentTimeMillis();
        try {
            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            HttpHeaders audioHeaders = new HttpHeaders();
            audioHeaders.setContentType(MediaType.parseMediaType("audio/wav"));
            org.springframework.core.io.ByteArrayResource audioResource = new org.springframework.core.io.ByteArrayResource(
                    audioData) {
                @Override
                public String getFilename() {
                    return "recording.wav";
                }
            };
            body.add("audio", new HttpEntity<>(audioResource, audioHeaders));
            body.add("target", targetText);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    localAsrEndpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            long latencyMs = System.currentTimeMillis() - start;
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // Handle the structure from the Python test script: success, data:
                // {transcribed, score, ...}
                if (Boolean.TRUE.equals(responseBody.get("success")) && responseBody.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    String text = (String) data.get("transcribed");
                    double score = ((Number) data.getOrDefault("score", 0)).doubleValue();
                    List<Map<String, Object>> wordDetails = null;
                    if (data.containsKey("word_details")) {
                        try {
                            wordDetails = (List<Map<String, Object>>) data.get("word_details");
                        } catch (Exception e) {
                            log.warn("Failed to parse word_details from ASR response: {}", e.getMessage());
                        }
                    }
                    return new AzureTranscriptionResult(text != null ? text : "", score, latencyMs, "local_asr", null,
                            wordDetails);
                }

                // Fallback for other structures
                String text = (String) responseBody.get("text");
                return new AzureTranscriptionResult(text != null ? text : "", 0, latencyMs, "local_asr", null, null);
            }
            return new AzureTranscriptionResult("", 0, latencyMs, "local_asr",
                    "Local ASR status: " + response.getStatusCode(), null);
        } catch (Exception e) {
            log.warn("Local ASR failed, fallback to secondary if available: {}", e.getMessage());
            return new AzureTranscriptionResult("", 0, System.currentTimeMillis() - start, "local_asr", e.getMessage(),
                    null);
        }
    }

    public Map<String, Object> chatWithGroq(String message) {
        String activeKey = systemConfigService.getValue("groq.api-key", groqApiKey);
        if (activeKey == null || activeKey.isBlank()) {
            throw new ApiException("CONFIG_ERROR",
                    "Groq API Key chưa được cấu hình. Vui lòng kiểm tra biến môi trường GROQ_API_KEY hoặc cấu hình trong hệ thống.");
        }
        return chatWithGroqOrFallback(message);
    }

    /**
     * Kiểm tra {@code audioUrl} trong body feedback — bắt buộc trỏ tới file {@code .webm} nếu có gửi.
     *
     * @param audioUrl URL file sau khi client upload (Firebase, …); null/blank → không kiểm tra
     *
     * <p><b>Note:</b> Đã fix BUG-001 — validate URL đồng bộ multipart, dùng trước {@link #provideFeedback}.</p>
     */
    public void validateFeedbackAudioUrl(String audioUrl) {
        WebmAudioValidator.validateAudioUrl(audioUrl);
    }

    /**
     * So sánh transcript ASR với câu mẫu, gọi Groq trả feedback sư phạm.
     *
     * @param transcribedText nội dung người học đã nói (từ ASR phía client/server)
     * @param targetText      câu chuẩn
     * @param focusErrorTag   tag lỗi phát âm ưu tiên (nullable)
     * @return accuracy, feedback, isCorrect, …
     */
    public Map<String, Object> provideFeedback(String transcribedText, String targetText, String focusErrorTag) {
        if (transcribedText == null || targetText == null || transcribedText.isBlank() || targetText.isBlank()) {
            return new HashMap<>(Map.of(
                    "isCorrect", false,
                    "accuracy", 0,
                    "errorType", "missing_input",
                    "feedback", "Vui lòng gửi đầy đủ văn bản nhận diện và văn bản mẫu.",
                    "shapeKey", "missing_input",
                    "score", 0,
                    "suggestion", "Vui lòng gửi đầy đủ văn bản nhận diện và văn bản mẫu.",
                    "errorDetail", "Thiếu dữ liệu đầu vào để chấm điểm.",
                    "aiProvider", "groq"));
        }

        String normalizedTranscribed = normalizeForComparison(transcribedText);
        String normalizedTarget = normalizeForComparison(targetText);
        if (!normalizedTranscribed.isBlank() && normalizedTranscribed.equals(normalizedTarget)) {
            return new HashMap<>(Map.of(
                    "isCorrect", true,
                    "accuracy", 100,
                    "errorType", "exact_match",
                    "feedback", "Bạn đọc đúng hoàn toàn. Hãy giữ nhịp nói tự nhiên và rõ âm cuối.",
                    "shapeKey", "exact_match",
                    "score", 100,
                    "suggestion", "Bạn đọc đúng hoàn toàn. Hãy giữ nhịp nói tự nhiên và rõ âm cuối.",
                    "errorDetail", "ASR khớp hoàn toàn với câu mẫu.",
                    "aiProvider", "groq"));
        }

        String prompt = buildFeedbackPrompt(transcribedText, targetText, focusErrorTag);
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
                if (raw != null)
                    return raw;
            } catch (Exception e) {
                lastError = e.getMessage();
            }
        }
        throw new ApiException("AI_ERROR", "Không thể gọi Groq API: " + lastError);
    }

    private List<String> groqModelCandidates() {
        List<String> candidates = new ArrayList<>();
        String activeModel = systemConfigService.getValue("groq.model", groqModel);
        if (activeModel != null && !activeModel.isBlank())
            candidates.add(activeModel.trim());
        String activeFallbacks = systemConfigService.getValue("groq.fallback-models", groqFallbackModels);
        if (activeFallbacks != null && !activeFallbacks.isBlank()) {
            for (String candidate : activeFallbacks.split(",")) {
                String normalized = candidate.trim();
                if (!normalized.isBlank() && !candidates.contains(normalized))
                    candidates.add(normalized);
            }
        }
        return candidates;
    }

    public String getAvailableErrorTagsText() {
        StringBuilder availableErrorTagsText = new StringBuilder();
        try {
            learningUnitRepository.findByType("ERROR_TAG").forEach(tag -> {
                String tagCode = tag.getErrorTag();
                if (tagCode == null || tagCode.isBlank()) {
                    try {
                        JsonNode node = objectMapper.readTree(tag.getMetadataJson());
                        tagCode = node.has("tag_code") ? node.get("tag_code").asText() : "N/A";
                    } catch (Exception e) {
                        tagCode = "N/A";
                    }
                }
                availableErrorTagsText.append("- Lỗi ").append(tag.getName()).append(" (Mã lỗi: ").append(tagCode).append(")\n");
            });
        } catch (Exception e) {
            log.warn("Could not fetch available error tags: {}", e.getMessage());
        }
        if (availableErrorTagsText.length() == 0) {
            availableErrorTagsText.append("- Lỗi phát âm N/L (Mã lỗi: L_N)\n")
                                  .append("- Lỗi phát âm S/X (Mã lỗi: S_X)\n")
                                  .append("- Lỗi phát âm TR/CH (Mã lỗi: CH_TR)\n")
                                  .append("- Lỗi phát âm D/R/GI (Mã lỗi: V_D_CONFUSION)\n");
        }
        return availableErrorTagsText.toString();
    }

    public String findErrorTagUnitId(String categorySearch) {
        if (categorySearch == null || categorySearch.isBlank()) return null;

        String suffix = categorySearch.contains("_")
                ? categorySearch.substring(categorySearch.lastIndexOf("_") + 1)
                : categorySearch;
        String normalizedSuffix = suffix.replaceAll("[^A-Za-z]", "").toUpperCase();
        char[] suffixChars = normalizedSuffix.toCharArray();
        java.util.Arrays.sort(suffixChars);
        String sortedSuffix = new String(suffixChars);

        log.info("[AIService] findErrorTagUnitId: category='{}', suffix='{}', sortedSuffix='{}'",
                categorySearch, normalizedSuffix, sortedSuffix);

        try {
            return learningUnitRepository.findByType("ERROR_TAG").stream()
                    .filter(lu -> {
                        if (lu == null || lu.getName() == null) return false;
                        String unitName = lu.getName().replaceAll("[^A-Za-z]", "").toUpperCase();
                        char[] unitChars = unitName.toCharArray();
                        java.util.Arrays.sort(unitChars);
                        String sortedUnit = new String(unitChars);
                        if (sortedSuffix.equals(sortedUnit)) {
                            return true;
                        }
                        try {
                            if (lu.getMetadataJson() != null && !lu.getMetadataJson().isBlank()) {
                                JsonNode node = objectMapper.readTree(lu.getMetadataJson());
                                if (node.has("tag_code")) {
                                    String tagCode = node.get("tag_code").asText("").replaceAll("[^A-Za-z]", "").toUpperCase();
                                    char[] tagChars = tagCode.toCharArray();
                                    java.util.Arrays.sort(tagChars);
                                    String sortedTag = new String(tagChars);
                                    if (sortedSuffix.equals(sortedTag)) {
                                        return true;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                        return false;
                    })
                    .map(lu -> lu.getId().toString())
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[AIService] Failed to resolve error tag dynamically: {}", e.getMessage());
            return null;
        }
    }

    private String buildFeedbackPrompt(String transcribedText, String targetText, String focusErrorTag) {
        String systemInstruction = systemConfigService.getValue("prompt.pronunciation-system-instruction", SYSTEM_INSTRUCTION);
        if (systemInstruction.contains("{availableErrorTags}")) {
            systemInstruction = systemInstruction.replace("{availableErrorTags}", getAvailableErrorTagsText());
        }

        String schemaInstruction = systemConfigService.getValue("prompt.pronunciation-schema-instruction", JSON_SCHEMA_INSTRUCTION);

        String focusTagName = null;
        String focusTagCode = null;
        if (focusErrorTag != null && !focusErrorTag.isBlank()) {
            try {
                UUID uuid = UUID.fromString(focusErrorTag);
                Optional<LearningUnit> tagOpt = learningUnitRepository.findById(uuid);
                if (tagOpt.isPresent()) {
                    LearningUnit tag = tagOpt.get();
                    focusTagName = tag.getName();
                    try {
                        JsonNode node = objectMapper.readTree(tag.getMetadataJson());
                        focusTagCode = node.has("tag_code") ? node.get("tag_code").asText() : tag.getErrorTag();
                    } catch (Exception e) {
                        focusTagCode = tag.getErrorTag();
                    }
                }
            } catch (IllegalArgumentException e) {
                // If it's not a UUID, treat it as a direct code/name
                focusTagCode = focusErrorTag;
                focusTagName = focusErrorTag;
            }
        }

        String errorTagsHintText = "";
        if (focusTagCode != null && !focusTagCode.isBlank()) {
            errorTagsHintText = "\nTarget error tag under test: " + focusTagCode + "\n";
        }

        String focusInstruction = "";
        if (focusTagName != null && !focusTagName.isBlank()) {
            focusInstruction = String.format(
                    "\nCRITICAL FOCUS: The user is specifically being tested for the error category: '%s' (Code: %s). " +
                            "Prioritize identifying and explaining issues related to this category. " +
                            "If the pronunciation is mostly correct but fails specifically on this phonetic rule, " +
                            "it MUST be marked as a regional error/mispronunciation and reflected in the accuracy score.",
                    focusTagName, focusTagCode != null ? focusTagCode : "N/A");
        }

        String defaultTemplate = "System instruction:\n{systemInstruction}\n\n" +
                "Input:\n" +
                "- rawText: {rawText}\n" +
                "- targetText: {targetText}\n\n" +
                "Output requirements:\n{schemaInstruction}\n\n" +
                "Rules:\n" +
                "1. Return JSON only.\n" +
                "2. feedback must be specific, natural, and actionable.\n" +
                "3. Do not say generic sentences like 'Phát âm chưa chính xác.' unless you also explain why.\n" +
                "4. If pronunciation is nearly correct, say what is close and what still needs fixing.\n" +
                "5. If there is a regional pronunciation issue, mention the likely sound pair and mouth/tongue/articulation clue.\n" +
                "{errorTagsHint}{focusInstruction}";
        String template = systemConfigService.getValue("prompt.pronunciation-prompt-template", defaultTemplate);

        return template
                .replace("{systemInstruction}", systemInstruction)
                .replace("{rawText}", transcribedText != null ? transcribedText : "")
                .replace("{targetText}", targetText != null ? targetText : "")
                .replace("{schemaInstruction}", schemaInstruction)
                .replace("{errorTagsHint}", errorTagsHintText)
                .replace("{focusInstruction}", focusInstruction);
    }

    private Map<String, Object> callGroqModel(String userMessage, String modelName) {
        String activeKey = systemConfigService.getValue("groq.api-key", groqApiKey);
        if (activeKey == null || activeKey.isBlank()) {
            throw new ApiException("CONFIG_ERROR",
                    "Groq API Key chưa được cấu hình. Vui lòng kiểm tra biến môi trường GROQ_API_KEY hoặc cấu hình trong hệ thống.");
        }

        String activeSystemInstruction = systemConfigService.getValue("prompt.pronunciation-system-instruction", SYSTEM_INSTRUCTION);
        if (activeSystemInstruction.contains("{availableErrorTags}")) {
            activeSystemInstruction = activeSystemInstruction.replace("{availableErrorTags}", getAvailableErrorTagsText());
        }

        String activeEndpoint = systemConfigService.getValue("groq.endpoint", groqEndpoint);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", activeSystemInstruction),
                Map.of("role", "user", "content", userMessage)));
        requestBody.put("temperature", 0.2);
        requestBody.put("response_format", Map.of("type", "json_object"));
        requestBody.put("top_p", 0.95);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(activeKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(activeEndpoint, HttpMethod.POST,
                new HttpEntity<>(requestBody, headers), new ParameterizedTypeReference<Map<String, Object>>() {
                });
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
        if (lastError instanceof RuntimeException runtimeException)
            throw runtimeException;
        throw new ApiException("AI_ERROR", "Không thể gọi Groq API với các model đã cấu hình.");
    }

    private String normalizeForComparison(String text) {
        if (text == null)
            return "";
        return text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private Map<String, Object> normalizeFeedbackResponse(Map<String, Object> raw, String transcribedText,
            String targetText, String reason) {
        if (raw == null) {
            throw new ApiException("AI_ERROR", "Groq trả về phản hồi rỗng.");
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
                raw.get("detectedError"), exactMatch ? "ASR khớp hoàn toàn với câu mẫu." : "");
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
        if (reason != null)
            result.put("ai_error_debug", reason);
        return result;
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean b)
            return b;
        if (value instanceof String s)
            return Boolean.parseBoolean(s);
        return defaultValue;
    }

    private int normalizeAccuracyValue(Object accuracyValue, Object scoreValue, int defaultValue) {
        Object value = accuracyValue != null ? accuracyValue : scoreValue;
        if (value instanceof Number number)
            return Math.max(0, Math.min(100, number.intValue()));
        if (value instanceof String text) {
            try {
                return Math.max(0, Math.min(100, Integer.parseInt(text.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value != null) {
                String text = value.toString().trim();
                if (!text.isBlank())
                    return text;
            }
        }
        return "";
    }

    private Map<String, Object> parseAndNormalizeOpenAiStyleResponse(Map<String, Object> responseBody,
            String targetText,
            String transcribedText, boolean useNormalization) {
        try {
            if (responseBody == null) {
                throw new ApiException("INTERNAL_SERVER_ERROR", "Groq AI returned empty response");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new ApiException("INTERNAL_SERVER_ERROR", "Groq AI returned no choices");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
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
                    return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
                    });
                } catch (Exception ignored) {
                    return new HashMap<>(Map.of("reply", cleanJson, "aiProvider", "groq"));
                }
            }
            JsonNode node = objectMapper.readTree(cleanJson);
            Map<String, Object> parsed = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
            });
            return normalizeFeedbackResponse(parsed, transcribedText, targetText, null);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> explainQuizAnswer(String question, String selectedAnswer, String correctAnswer,
            String skillType, String transcript, String correctSentence) {
        boolean isTimeout = selectedAnswer.contains("chưa chọn đáp án");
        String resultStatus = selectedAnswer.equalsIgnoreCase(correctAnswer) ? "CHÍNH XÁC" : "CHƯA ĐÚNG";

        String template = systemConfigService.getValue("prompt.quiz-explanation", 
                "Bạn là giáo viên dạy Tiếng Việt vui nhộn và tận tâm. Hãy giải thích ngắn gọn (1-3 câu) lý do vì sao đáp án là {status}. {timeoutDetail} Câu hỏi: \"{question}\". Người dùng chọn: \"{selectedAnswer}\". Đáp án đúng là: \"{correctAnswer}\". Kỹ năng: {skillType}. {hearingDetail} {correctDetail} Hãy giúp người dùng hiểu rõ kiến thức một cách thân thiện. Trả về JSON có field 'explanation'.");

        String timeoutDetail = isTimeout
                ? "Đặc biệt lưu ý: Người dùng đã hết thời gian và CHƯA KỊP CHỌN ĐÁP ÁN. Hãy bắt đầu bằng việc nhắc nhở người dùng chưa chọn đáp án, sau đó chỉ ra đáp án đúng và giải thích. "
                : "";
        String hearingDetail = (transcript != null && !transcript.isBlank())
                ? String.format("Nội dung bài nghe (transcript): \"%s\". ", transcript)
                : "";
        String correctDetail = (correctSentence != null && !correctSentence.isBlank())
                ? String.format("Câu đúng: \"%s\". Hãy so sánh với transcript để chỉ ra từ bị phát âm sai/ngọng (ví dụ N thành L). ", correctSentence)
                : "";

        String prompt = template
                .replace("{status}", resultStatus)
                .replace("{timeoutDetail}", timeoutDetail)
                .replace("{question}", question != null ? question : "")
                .replace("{selectedAnswer}", selectedAnswer != null ? selectedAnswer : "")
                .replace("{correctAnswer}", correctAnswer != null ? correctAnswer : "")
                .replace("{skillType}", skillType != null ? skillType : "")
                .replace("{hearingDetail}", hearingDetail)
                .replace("{correctDetail}", correctDetail);

        return chatWithGroq(prompt);
    }

    public record AzureTranscriptionResult(String transcript, double pronunciationAccuracy, long latencyMs,
            String provider, String error, List<Map<String, Object>> wordDetails) {
    }
}
