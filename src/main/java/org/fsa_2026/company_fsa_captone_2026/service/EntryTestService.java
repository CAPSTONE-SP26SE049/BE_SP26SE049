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
import org.fsa_2026.company_fsa_captone_2026.repository.CustomLearningPathRepository;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.CustomLearningPath;
import org.fsa_2026.company_fsa_captone_2026.entity.CustomPathLevel;
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
    private final FirebaseStorageService firebaseStorageService;

    private final LearningUnitRepository learningUnitRepository;
    private final AccountLearningUnitRepository accountLearningUnitRepository;
    private final CustomLearningPathService customLearningPathService;
    private final AIService aiService;
    private final CustomLearningPathRepository customLearningPathRepository;
    private final RoadmapRuleService roadmapRuleService;


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

        // Nếu chưa đủ 10 câu (do DB thiếu hoặc truyền region sai/null), lấy trộn như
        // fallback
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
    public Map<String, Object> analyzeEntryTestStep(UUID questionId, org.springframework.web.multipart.MultipartFile audioFile) throws java.io.IOException {
        EntryTestQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        byte[] audioData = audioFile.getBytes();
        // === Sử dụng đúng pipeline của Quiz Speaking: Azure Speech + Groq AI ===
        Map<String, Object> quizResult = aiService.evaluatePronunciation(audioData, question.getTargetText());

        // Upload to Firebase
        try {
            String audioUrl = firebaseStorageService.uploadFile(audioFile, "entry-test");
            quizResult.put("audioUrl", audioUrl);
            log.info("Uploaded entry test audio to: {}", audioUrl);
        } catch (Exception e) {
            log.error("Failed to upload entry test audio to Firebase", e);
        }

        // === Wrap kết quả để thêm thông tin vùng miền phục vụ chẩn đoán Entry Test ===
        Map<String, Object> diagnosis = new HashMap<>(quizResult);
        diagnosis.put("regionCategory", question.getRegionCategory());
        diagnosis.put("targetText", question.getTargetText());
        diagnosis.put("questionId", questionId.toString());

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

        // Giai đoạn 2: Lọc lỗi và format lại trong JSON (đã có trong serializeDetails)
        // We no longer need to save to EntryTestResultDetail table, we just serialize them.
        
        // Mark account as having completed the entry test
        account.setHasDoneEntryTest(true);
        accountRepository.save(account);

        // Giai đoạn 3: Mở khóa Level & Lộ trình cá nhân hóa
        unlockLevelsBasedOnScore(account, detectedRegion, overallScore);
        assignPersonalRoadmap(account, detectedRegion, result, stepResults);

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

    private void assignPersonalRoadmap(Account account, RegionCode region, EntryTestResult result, List<Map<String, Object>> stepResults) {
        Map<String, Integer> wrongCounts = new HashMap<>();
        Map<String, Integer> nearCounts = new HashMap<>();
        
        for (Map<String, Object> res : stepResults) {
            String questionIdStr = (String) res.get("questionId");
            if (questionIdStr == null) continue;
            UUID qId;
            try {
                qId = UUID.fromString(questionIdStr);
            } catch (Exception e) { continue; }
            EntryTestQuestion question = questionRepository.findById(qId).orElse(null);
            if (question == null) continue;
            
            Object wordDetailsObj = res.get("word_details");
            if (wordDetailsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> wordDetails = (List<Map<String, Object>>) wordDetailsObj;
                for (Map<String, Object> wd : wordDetails) {
                    String word = (String) wd.get("word");
                    String status = (String) wd.get("status");
                    if (word == null || status == null) continue;
                    status = status.toLowerCase();
                    if (!status.equals("wrong") && !status.equals("near")) continue;

                    String errorCat = null;
                    EntryTestRegionCategory category = question.getRegionCategory();
                    String lw = word.toLowerCase();
                    if (category == EntryTestRegionCategory.NORTH_NL) {
                        if (lw.startsWith("l") || lw.startsWith("n")) errorCat = "L_N";
                    } else if (category == EntryTestRegionCategory.SOUTH_TRCH) {
                        if (lw.startsWith("tr") || lw.startsWith("ch")) errorCat = "TR_CH";
                    } else if (category == EntryTestRegionCategory.CENTRAL_DGIR) {
                        if (lw.startsWith("d") || lw.startsWith("gi") || lw.startsWith("r")) errorCat = "D_GI_R";
                    }
                    
                    if (errorCat != null) {
                        if ("wrong".equals(status)) {
                            wrongCounts.put(errorCat, wrongCounts.getOrDefault(errorCat, 0) + 1);
                        } else {
                            nearCounts.put(errorCat, nearCounts.getOrDefault(errorCat, 0) + 1);
                        }
                    }
                }
            }
        }

        Map<String, Double> categoryAccuracy = new HashMap<>();
        Set<String> allCategories = new HashSet<>(wrongCounts.keySet());
        allCategories.addAll(nearCounts.keySet());
        
        for (String cat : allCategories) {
            int wrong = wrongCounts.getOrDefault(cat, 0);
            int near = nearCounts.getOrDefault(cat, 0);
            
            long totalWords = 10; // Fallback approximation
            double errorRate = (wrong * 1.0 + near * 0.5) / totalWords;
            double accuracy = (1.0 - errorRate) * 100;
            categoryAccuracy.put(cat, Math.max(0, accuracy));
        }

        List<String> sortedCategories = categoryAccuracy.keySet().stream()
                .sorted((c1, c2) -> Integer.compare(wrongCounts.getOrDefault(c2, 0), wrongCounts.getOrDefault(c1, 0)))
                .collect(Collectors.toList());

        CustomLearningPath customPath = CustomLearningPath.builder()
                .student(account)
                .title("Lộ trình Học cá nhân hóa")
                .description("Lộ trình được tạo tự động bởi AI dựa trên kết quả kiểm tra đầu vào.")
                .isAiGenerated(true)
                .aiFeedback("Đang phân tích...")
                .targetLevel(region.name())
                .levels(new ArrayList<>())
                .build();
        
        customPath = customLearningPathRepository.save(customPath);

        int orderIndex = 1;
        for (String cat : sortedCategories) {
            double acc = categoryAccuracy.get(cat);
            List<String> difficultiesToAssign = roadmapRuleService.getDifficultiesForScore(acc);

            if (!difficultiesToAssign.isEmpty()) {
                for (String diff : difficultiesToAssign) {
                    List<LearningUnit> units = learningUnitRepository.findByType("LEVEL").stream()
                            .filter(u -> cat.equalsIgnoreCase(u.getErrorTag())
                                    && diff.equalsIgnoreCase(u.getDifficultyLevel()))
                            .collect(Collectors.toList());

                    for (LearningUnit unit : units) {
                        customPath.addLevel(unit, orderIndex++);
                    }
                }
            }
        }
        customPath = customLearningPathRepository.save(customPath);

        // Giai đoạn 4: Tích hợp AI sinh nhận xét
        StringBuilder promptBuilder = new StringBuilder("Học viên mắc các lỗi sau trong phát âm: ");
        if (categoryAccuracy.isEmpty()) {
            promptBuilder.append("Không có lỗi ngọng vùng miền nghiêm trọng. ");
        } else {
            for (String cat : sortedCategories) {
                promptBuilder.append("Lỗi ").append(cat).append(" với độ chính xác ")
                        .append(String.format("%.1f", categoryAccuracy.get(cat))).append("% (có ")
                        .append(wrongCounts.getOrDefault(cat, 0)).append(" từ sai hoàn toàn); ");
            }
        }
        promptBuilder.append(
                "Hãy viết 1 đoạn 3-4 câu nhận xét ngắn gọn, cổ vũ học viên và khuyên học viên nên ưu tiên học lỗi nào trước (dựa trên % độ chính xác thấp nhất).");

        try {
            Map<String, Object> aiResponse = aiService.chatWithGroq(promptBuilder.toString());
            String aiFeedback = "Bạn cần cố gắng luyện tập thêm!";
            if (aiResponse.containsKey("reply")) {
                aiFeedback = (String) aiResponse.get("reply");
            } else if (aiResponse.containsKey("feedback")) {
                aiFeedback = (String) aiResponse.get("feedback");
            } else if (aiResponse.containsKey("explanation")) {
                aiFeedback = (String) aiResponse.get("explanation");
            }
            customPath.setAiFeedback(aiFeedback);
            customLearningPathRepository.save(customPath);
        } catch (Exception e) {
            log.error("Error generating AI analysis for custom path", e);
            customPath.setAiFeedback("Hệ thống AI đang bận. Dựa trên kết quả bài test, bạn đã được phân bổ lộ trình học phù hợp với lỗi phát âm của mình.");
            customLearningPathRepository.save(customPath);
        }
    }

    private long countTargetWords(String text, String errorCategory) {
        if (text == null)
            return 0;
        String[] words = text.toLowerCase().replaceAll("[^\\p{L}\\s]", "").split("\\s+");
        long count = 0;
        for (String w : words) {
            if (w.isEmpty())
                continue;
            if ("L_N".equals(errorCategory) && (w.startsWith("l") || w.startsWith("n")))
                count++;
            else if ("TR_CH".equals(errorCategory) && (w.startsWith("tr") || w.startsWith("ch")))
                count++;
            else if ("D_GI_R".equals(errorCategory) && (w.startsWith("d") || w.startsWith("gi") || w.startsWith("r")))
                count++;
            else if ("S_X".equals(errorCategory) && (w.startsWith("s") || w.startsWith("x")))
                count++;
        }
        return count;
    }

    /**
     * Robust lookup for the dialect LearningUnit by trying multiple name variations
     */
    private LearningUnit findDialectByRegionCode(RegionCode region) {
        String internalName = region.name(); // "NORTH", "CENTRAL", "SOUTH"
        Optional<LearningUnit> byInternal = learningUnitRepository.findByTypeAndNameIgnoreCase("DIALECT", internalName);
        if (byInternal.isPresent())
            return byInternal.get();

        // Try Vietnamese names
        String vnName = switch (region) {
            case NORTH -> "Miền Bắc";
            case CENTRAL -> "Miền Trung";
            case SOUTH -> "Miền Nam";
        };
        Optional<LearningUnit> byVnName = learningUnitRepository.findByTypeAndNameIgnoreCase("DIALECT", vnName);
        if (byVnName.isPresent())
            return byVnName.get();

        // Last resort search all dialects and check if they contain the keyword
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


    private EntryTestQuestionResponse mapToResponse(EntryTestQuestion question) {
        return EntryTestQuestionResponse.builder()
                .id(question.getId())
                .targetText(question.getTargetText())
                .regionCategory(question.getRegionCategory())
                .build();
    }
}
