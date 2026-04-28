package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.EntryTestQuestionRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.EntryTestQuestionResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.EntryTestResultResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.EntryTestQuestion;
import org.fsa_2026.company_fsa_captone_2026.entity.EntryTestResult;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.EntryTestRegionCategory;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RegionCode;
import org.fsa_2026.company_fsa_captone_2026.exception.ResourceNotFoundException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.EntryTestQuestionRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.EntryTestResultRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntryTestService {

    private final EntryTestQuestionRepository questionRepository;
    private final EntryTestResultRepository resultRepository;
    private final AccountRepository accountRepository;
    private final LearningUnitRepository learningUnitRepository;
    private final AccountLearningUnitRepository accountLearningUnitRepository;
    private final CustomLearningPathService customLearningPathService;
    private final AIService aiService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${asr.local.endpoint:http://localhost:8000/asr}")
    private String localAsrEndpoint;

    // CRUD Methods
    public List<EntryTestQuestionResponse> getAllQuestions() {
        return questionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public EntryTestQuestionResponse getQuestionById(UUID id) {
        EntryTestQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entry test question not found"));
        return mapToResponse(question);
    }

    @Transactional
    public EntryTestQuestionResponse createQuestion(EntryTestQuestionRequest request) {
        EntryTestQuestion question = EntryTestQuestion.builder()
                .targetText(request.getTargetText())
                .regionCategory(request.getRegionCategory())
                .build();
        return mapToResponse(questionRepository.save(question));
    }

    @Transactional
    public EntryTestQuestionResponse updateQuestion(UUID id, EntryTestQuestionRequest request) {
        EntryTestQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entry test question not found"));
        question.setTargetText(request.getTargetText());
        question.setRegionCategory(request.getRegionCategory());
        return mapToResponse(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(UUID id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Entry test question not found");
        }
        questionRepository.deleteById(id);
    }

    // Admin Methods
    @Transactional(readOnly = true)
    public List<EntryTestResultResponse> getAllResults() {
        return resultRepository.findAll().stream()
                .map(r -> EntryTestResultResponse.builder()
                        .id(r.getId())
                        .overallScore(r.getOverallScore())
                        .detectedRegion(r.getDetectedRegion() != null ? r.getDetectedRegion().name() : null)
                        .totalQuestions(r.getTotalQuestions())
                        .createdAt(r.getCreatedAt())
                        .userEmail(r.getAccount() != null ? r.getAccount().getEmail() : null)
                        .userFullName(r.getAccount() != null ? r.getAccount().getFullName() : null)
                        .details(r.getDetails())
                        .build())
                .collect(Collectors.toList());
    }

    // Placement Set Logic
    public List<EntryTestQuestionResponse> getPlacementSet(String region) {
        List<EntryTestQuestion> questions = new ArrayList<>();

        if (region != null && !region.trim().isEmpty()) {
            // Lấy 10 câu ngẫu nhiên của miền được chọn
            String categoryMap = switch (region.toUpperCase()) {
                case "NORTH" -> EntryTestRegionCategory.NORTH_NL.name();
                case "CENTRAL" -> EntryTestRegionCategory.CENTRAL_DGIR.name();
                case "SOUTH" -> EntryTestRegionCategory.SOUTH_TRCH.name();
                default -> null;
            };

            if (categoryMap != null) {
                questions.addAll(questionRepository.findRandomByRegion(categoryMap, 10));
            }
        }

        // Nếu chưa đủ 10 câu (do DB thiếu hoặc truyền region sai/null), lấy trộn như fallback
        if (questions.size() < 10) {
            questions.clear();
            // Ensure 3 from each region to get 9, then 1 more random for 10
            questions.addAll(questionRepository.findRandomByRegion(EntryTestRegionCategory.NORTH_NL.name(), 3));
            questions.addAll(questionRepository.findRandomByRegion(EntryTestRegionCategory.CENTRAL_DGIR.name(), 3));
            questions.addAll(questionRepository.findRandomByRegion(EntryTestRegionCategory.SOUTH_TRCH.name(), 3));

            // Get all questions to pick one more random one that is not already in the list
            List<UUID> existingIds = questions.stream().map(EntryTestQuestion::getId).collect(Collectors.toList());
            List<EntryTestQuestion> remaining = questionRepository.findAll().stream()
                    .filter(q -> !existingIds.contains(q.getId()))
                    .collect(Collectors.toList());

            if (!remaining.isEmpty()) {
                Collections.shuffle(remaining);
                questions.add(remaining.get(0));
            }
        }

        Collections.shuffle(questions);

        return questions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> analyzeEntryTestStep(UUID questionId, byte[] audioData) {
        EntryTestQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        // === Sử dụng đúng pipeline của Quiz Speaking: Azure Speech + Groq AI ===
        Map<String, Object> quizResult = aiService.evaluatePronunciation(audioData, question.getTargetText());

        // === Wrap kết quả để thêm thông tin vùng miền phục vụ chẩn đoán Entry Test ===
        Map<String, Object> diagnosis = new HashMap<>(quizResult);
        diagnosis.put("regionCategory", question.getRegionCategory());
        diagnosis.put("targetText", question.getTargetText());

        // === Đồng bộ field rawText với Quiz Speaking (azureTranscript) ===
        String rawText = (String) quizResult.getOrDefault("azureTranscript", "");
        diagnosis.put("rawText", rawText);

        // === Chuẩn hóa accuracy về thang 0-100 (đồng bộ với Quiz Speaking) ===
        Object accObj = quizResult.get("accuracy");
        double accuracyValue = 0;
        if (accObj instanceof Number num) {
            accuracyValue = num.doubleValue();
            if (accuracyValue <= 1.0 && accuracyValue > 0) {
                accuracyValue = accuracyValue * 100; // scale 0-1 lên 0-100
            }
        }
        diagnosis.put("accuracy", accuracyValue);

        // === Detect lỗi phát âm đặc trưng vùng miền từ AI response ===
        // Quiz Speaking trả về detectedError/errorType; Entry Test dùng để classify
        // isRegional
        boolean isRegional = false;
        String detectedError = firstNonBlankStr(
                quizResult.get("detectedError"),
                quizResult.get("errorType"),
                quizResult.get("errorDetail"));
        if (detectedError != null && !detectedError.isBlank()) {
            String errorLow = detectedError.toLowerCase();
            // Các lỗi phát âm đặc trưng vùng miền Việt Nam: N/L, S/X, TR/CH, D/R/GI
            isRegional = errorLow.contains("n/l") || errorLow.contains("l/n")
                    || errorLow.contains("s/x") || errorLow.contains("x/s")
                    || errorLow.contains("tr/ch") || errorLow.contains("ch/tr")
                    || errorLow.contains("d/r") || errorLow.contains("r/d")
                    || errorLow.contains("d/gi") || errorLow.contains("gi/d")
                    || errorLow.contains("regional") || errorLow.contains("vùng miền")
                    || errorLow.contains("đặc trưng");
        }
        // Nếu AI đã trả về isRegional thì ưu tiên dùng giá trị đó
        Object aiIsRegional = quizResult.get("isRegional");
        if (aiIsRegional instanceof Boolean b) {
            isRegional = b || isRegional; // Union: nếu 1 trong 2 true thì true
        }
        diagnosis.put("isRegional", isRegional);
        diagnosis.put("detectedError", detectedError != null ? detectedError : "");

        return diagnosis;
    }

    /** Tiện ích: lấy String đầu tiên không rỗng từ danh sách Object */
    private String firstNonBlankStr(Object... values) {
        for (Object v : values) {
            if (v != null) {
                String s = v.toString().trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s))
                    return s;
            }
        }
        return null;
    }

    @Transactional
    public EntryTestResult saveFinalResult(String email, List<Map<String, Object>> stepResults) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        double totalAccuracy = 0;
        Map<RegionCode, Integer> regionErrorCount = new HashMap<>();
        regionErrorCount.put(RegionCode.NORTH, 0);
        regionErrorCount.put(RegionCode.CENTRAL, 0);
        regionErrorCount.put(RegionCode.SOUTH, 0);

        for (Map<String, Object> res : stepResults) {
            double accuracy = ((Number) res.getOrDefault("accuracy", 0)).doubleValue();
            totalAccuracy += accuracy;

            boolean isRegional = Boolean.TRUE.equals(res.get("isRegional"));
            if (isRegional) {
                Object catObj = res.get("regionCategory");
                EntryTestRegionCategory category = null;
                if (catObj instanceof String) {
                    category = EntryTestRegionCategory.valueOf((String) catObj);
                } else if (catObj instanceof EntryTestRegionCategory) {
                    category = (EntryTestRegionCategory) catObj;
                }

                if (category == EntryTestRegionCategory.NORTH_NL)
                    regionErrorCount.put(RegionCode.NORTH, regionErrorCount.get(RegionCode.NORTH) + 1);
                else if (category == EntryTestRegionCategory.CENTRAL_DGIR)
                    regionErrorCount.put(RegionCode.CENTRAL, regionErrorCount.get(RegionCode.CENTRAL) + 1);
                else if (category == EntryTestRegionCategory.SOUTH_TRCH)
                    regionErrorCount.put(RegionCode.SOUTH, regionErrorCount.get(RegionCode.SOUTH) + 1);
            }
        }

        double overallScore = totalAccuracy / stepResults.size();

        // Determine detectedRegion (region with most regional errors)
        RegionCode detectedRegion = regionErrorCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(RegionCode.NORTH);

        // Fallback: if no regional errors, pick region with lowest average accuracy
        if (regionErrorCount.values().stream().allMatch(v -> v == 0)) {
            Map<RegionCode, Double> regionAccuracy = new HashMap<>();
            regionAccuracy.put(RegionCode.NORTH, 0.0);
            regionAccuracy.put(RegionCode.CENTRAL, 0.0);
            regionAccuracy.put(RegionCode.SOUTH, 0.0);
            Map<RegionCode, Integer> regionCount = new HashMap<>();
            regionCount.put(RegionCode.NORTH, 0);
            regionCount.put(RegionCode.CENTRAL, 0);
            regionCount.put(RegionCode.SOUTH, 0);

            for (Map<String, Object> res : stepResults) {
                Object catObj = res.get("regionCategory");
                EntryTestRegionCategory category = null;
                if (catObj instanceof String) {
                    category = EntryTestRegionCategory.valueOf((String) catObj);
                } else if (catObj instanceof EntryTestRegionCategory) {
                    category = (EntryTestRegionCategory) catObj;
                }

                RegionCode rc = mapCategoryToRegionCode(category);
                regionAccuracy.put(rc,
                        regionAccuracy.get(rc) + ((Number) res.getOrDefault("accuracy", 0)).doubleValue());
                regionCount.put(rc, regionCount.get(rc) + 1);
            }

            detectedRegion = regionAccuracy.entrySet().stream()
                    .min(Comparator.comparingDouble(e -> e.getValue() / Math.max(1, regionCount.get(e.getKey()))))
                    .map(Map.Entry::getKey)
                    .orElse(RegionCode.NORTH);
        }

        EntryTestResult result = EntryTestResult.builder()
                .account(account)
                .overallScore(overallScore)
                .detectedRegion(detectedRegion)
                .totalQuestions(stepResults.size())
                .details(serializeDetails(stepResults))
                .build();

        result = resultRepository.save(result);

        // Mark account as having completed the entry test
        account.setHasDoneEntryTest(true);
        accountRepository.save(account);

        // Giai đoạn 3: Mở khóa Level & Lộ trình cá nhân hóa
        unlockLevelsBasedOnScore(account, detectedRegion, overallScore);
        assignLearningPath(account, detectedRegion);

        return result;
    }

    private RegionCode mapCategoryToRegionCode(EntryTestRegionCategory category) {
        if (category == EntryTestRegionCategory.NORTH_NL)
            return RegionCode.NORTH;
        if (category == EntryTestRegionCategory.CENTRAL_DGIR)
            return RegionCode.CENTRAL;
        return RegionCode.SOUTH;
    }

    private void unlockLevelsBasedOnScore(Account account, RegionCode region, double score) {
        int levelsToUnlock = 1;
        if (score >= 100)
            levelsToUnlock = 4;
        else if (score >= 80)
            levelsToUnlock = 3;
        else if (score >= 60)
            levelsToUnlock = 2;

        // Use more robust dialect lookup
        LearningUnit dialect = findDialectByRegionCode(region);

        if (dialect != null) {
            List<LearningUnit> levels = learningUnitRepository.findByParentAndType(dialect, "LEVEL");
            levels.sort(Comparator.comparingInt(lu -> {
                try {
                    // Extract level_order from JSON metadata
                    @SuppressWarnings("unchecked")
                    Map<String, Object> meta = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(lu.getMetadataJson(), Map.class);
                    return ((Number) meta.getOrDefault("level_order", 0)).intValue();
                } catch (Exception e) {
                    return 0;
                }
            }));

            for (int i = 0; i < levels.size(); i++) {
                LearningUnit level = levels.get(i);
                org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit alu = accountLearningUnitRepository
                        .findByAccountIdAndLearningUnitId(account.getId(), level.getId())
                        .orElse(org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit.builder()
                                .account(account)
                                .learningUnit(level)
                                .build());

                if (i < levelsToUnlock) {
                    alu.setIsUnlocked(true);
                } else {
                    alu.setIsUnlocked(false);
                }
                accountLearningUnitRepository.save(alu);
            }
        }
    }

    private void assignLearningPath(Account account, RegionCode region) {
        // Automatically assign chapters of the detected region to LearningPath
        LearningUnit dialect = findDialectByRegionCode(region);

        if (dialect != null) {
            // Simply creating or updating a CustomLearningPath for this student
            customLearningPathService.createAutoPath(account, dialect);
        }
    }

    /** Robust lookup for the dialect LearningUnit by trying multiple name variations */
    private LearningUnit findDialectByRegionCode(RegionCode region) {
        String internalName = region.name(); // "NORTH", "CENTRAL", "SOUTH"
        Optional<LearningUnit> byInternal = learningUnitRepository.findByTypeAndNameIgnoreCase("DIALECT", internalName);
        if (byInternal.isPresent()) return byInternal.get();

        // Try Vietnamese names
        String vnName = switch (region) {
            case NORTH -> "Miền Bắc";
            case CENTRAL -> "Miền Trung";
            case SOUTH -> "Miền Nam";
        };
        Optional<LearningUnit> byVnName = learningUnitRepository.findByTypeAndNameIgnoreCase("DIALECT", vnName);
        if (byVnName.isPresent()) return byVnName.get();

        // Last resort: search all dialects and check if they contain the keyword
        return learningUnitRepository.findByType("DIALECT").stream()
                .filter(lu -> {
                    String name = lu.getName().toUpperCase();
                    return name.contains(internalName) || name.contains(vnName.toUpperCase());
                })
                .findFirst()
                .orElse(null);
    }

    private String serializeDetails(List<Map<String, Object>> details) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(details);
        } catch (Exception e) {
            log.error("Failed to serialize entry test details", e);
            return details.toString();
        }
    }

    private String transcribeWithLocalAsr(byte[] audioData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(audioData) {
                @Override
                public String getFilename() {
                    return "audio.wav";
                }
            };
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(localAsrEndpoint, HttpMethod.POST,
                    requestEntity, new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            return response.getBody() != null ? (String) response.getBody().get("text") : "";
        } catch (Exception e) {
            log.error("Local ASR failed", e);
            return "Lỗi nhận diện";
        }
    }

    private EntryTestQuestionResponse mapToResponse(EntryTestQuestion question) {
        return EntryTestQuestionResponse.builder()
                .id(question.getId())
                .targetText(question.getTargetText())
                .regionCategory(question.getRegionCategory())
                .build();
    }
}
