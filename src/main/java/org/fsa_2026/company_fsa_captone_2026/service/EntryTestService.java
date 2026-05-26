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
import org.fsa_2026.company_fsa_captone_2026.common.WebmAudioValidator;
import org.fsa_2026.company_fsa_captone_2026.exception.BadRequestException;
import org.fsa_2026.company_fsa_captone_2026.exception.ResourceNotFoundException;
import org.springframework.web.multipart.MultipartFile;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.EntryTestQuestionRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.EntryTestResultRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.CustomLearningPathRepository;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.CustomLearningPath;
import org.fsa_2026.company_fsa_captone_2026.entity.CustomPathLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SystemConfigService systemConfigService;


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

    private static final String MSG_INVALID_REGION =
            "Giá trị region không hợp lệ. Chấp nhận: NORTH, CENTRAL, SOUTH";

    private static final Set<String> VALID_PLACEMENT_REGIONS =
            Set.of("NORTH", "CENTRAL", "SOUTH");

    /**
     * Xây dựng bộ câu hỏi placement (~10 câu) theo miền hoặc trộn 3 miền khi không chỉ định region.
     *
     * @param region    query param tùy chọn; null/blank → đọc {@code region} từ profile user
     * @param userEmail email từ JWT (Security Context)
     * @return danh sách {@link EntryTestQuestionResponse} đã shuffle
     *
     * <p><b>Note (BUG-004):</b> Query {@code region} sai (vd. HANG_NGAY) → 400. Query trống + profile trống → trộn 3 miền.</p>
     */
    public List<EntryTestQuestionResponse> getPlacementSet(String region, String userEmail) {
        final List<EntryTestQuestion> questions;
        final String effectiveRegion;

        if (region != null && !region.isBlank()) {
            effectiveRegion = region.trim();
        } else {
            effectiveRegion = resolvePlacementRegionFromAccount(userEmail);
        }

        if (effectiveRegion == null || effectiveRegion.isBlank()) {
            questions = fetchMixedPlacementQuestions();
        } else {
            String regionKey = effectiveRegion.toUpperCase(Locale.ROOT);
            if (!VALID_PLACEMENT_REGIONS.contains(regionKey)) {
                throw new BadRequestException(MSG_INVALID_REGION);
            }

            String categoryMap = mapPlacementRegionToCategory(regionKey);
            questions = new ArrayList<>(questionRepository.findRandomByRegion(categoryMap, 10));

            if (questions.size() < 10) {
                questions.clear();
                questions.addAll(fetchMixedPlacementQuestions());
            }
        }

        Collections.shuffle(questions);
        return questions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy mã miền placement (NORTH/CENTRAL/SOUTH) từ profile {@link Account#region} của user đăng nhập.
     */
    private String resolvePlacementRegionFromAccount(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return null;
        }
        return accountRepository.findByEmail(userEmail)
                .map(Account::getRegion)
                .map(this::normalizeAccountRegionToPlacementKey)
                .orElse(null);
    }

    /**
     * Chuẩn hóa giá trị region lưu DB (BAC, north, …) sang NORTH | CENTRAL | SOUTH.
     */
    private String normalizeAccountRegionToPlacementKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "NORTH", "BAC", "MIEN_BAC", "MIỀN_BẮC", "BẮC" -> "NORTH";
            case "CENTRAL", "TRUNG", "MIEN_TRUNG", "MIỀN_TRUNG" -> "CENTRAL";
            case "SOUTH", "NAM", "MIEN_NAM", "MIỀN_NAM" -> "SOUTH";
            default -> VALID_PLACEMENT_REGIONS.contains(key) ? key : null;
        };
    }

    /**
     * Fallback: 3 câu Bắc + 3 Trung + 3 Nam + 1 câu ngẫu nhiên còn lại (tối đa ~10 câu).
     * Chỉ được gọi khi {@code region} null/blank hoặc miền hợp lệ nhưng thiếu dữ liệu DB.
     */
    private List<EntryTestQuestion> fetchMixedPlacementQuestions() {
        List<EntryTestQuestion> questions = new ArrayList<>();
        questions.addAll(questionRepository.findRandomByRegion(EntryTestRegionCategory.NORTH_NL.name(), 3));
        questions.addAll(questionRepository.findRandomByRegion(EntryTestRegionCategory.CENTRAL_DGIR.name(), 3));
        questions.addAll(questionRepository.findRandomByRegion(EntryTestRegionCategory.SOUTH_TRCH.name(), 3));

        List<UUID> existingIds = questions.stream().map(EntryTestQuestion::getId).collect(Collectors.toList());
        List<EntryTestQuestion> remaining = questionRepository.findAll().stream()
                .filter(q -> !existingIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (!remaining.isEmpty()) {
            Collections.shuffle(remaining);
            questions.add(remaining.get(0));
        }
        return questions;
    }

    /**
     * Chẩn đoán một lượt phát âm trong entry test: ASR + Groq AI, upload Firebase, gắn metadata miền.
     *
     * @param questionId ID câu hỏi placement
     * @param audioFile  file âm thanh multipart (chỉ chấp nhận {@code .webm})
     * @return map chẩn đoán (accuracy, rawText, isRegional, regionCategory, audioUrl, …)
     * @throws java.io.IOException khi đọc bytes từ file
     * @throws ResourceNotFoundException nếu {@code questionId} không có trong DB → HTTP 404 (BUG-002)
     * @throws BadRequestException nếu file rỗng hoặc sai định dạng (BUG-001)
     *
     * <p><b>Note:</b> Đã bổ sung {@link WebmAudioValidator#validateMultipart} — chặn file rác, chỉ nhận {@code .webm} (BUG-001).
     * Pipeline AI ({@link AIService#evaluatePronunciation}) giữ nguyên sau bước validate.</p>
     */
    @Transactional
    public Map<String, Object> analyzeEntryTestStep(UUID questionId, MultipartFile audioFile) throws java.io.IOException {
        WebmAudioValidator.validateMultipart(audioFile);

        EntryTestQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        byte[] audioData = audioFile.getBytes();
        // Resolve the specific tag code for focus (e.g. L_N for NORTH_NL)
        String tagCode = findErrorTagUnitId(question.getRegionCategory().name());
        
        // === Sử dụng đúng pipeline của Quiz Speaking: Azure Speech + Groq AI ===
        Map<String, Object> quizResult = aiService.evaluatePronunciation(audioData, question.getTargetText(), tagCode);

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
            // Lấy toàn bộ danh sách error tag hiện có từ database để đối chiếu động
            try {
                List<LearningUnit> errorTags = learningUnitRepository.findByType("ERROR_TAG");
                for (LearningUnit tag : errorTags) {
                    String tagNameLow = tag.getName() != null ? tag.getName().toLowerCase() : "";
                    String tagCodeLow = tag.getErrorTag() != null ? tag.getErrorTag().toLowerCase() : "";
                    
                    // Nếu lỗi do AI nhận diện chứa tên hoặc mã của bất kỳ error tag nào trong hệ thống
                    if ((!tagNameLow.isEmpty() && errorLow.contains(tagNameLow)) || 
                        (!tagCodeLow.isEmpty() && errorLow.contains(tagCodeLow)) ||
                        errorLow.contains(tagCodeLow.replace("_", "/")) ||
                        errorLow.contains(tagCodeLow.replace("_", "-"))) {
                        isRegional = true;
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to check regional error dynamically, fallback to standard contains: {}", e.getMessage());
            }
            
            // Standard fallback keywords
            if (!isRegional) {
                isRegional = errorLow.contains("regional") || errorLow.contains("vùng miền")
                        || errorLow.contains("đặc trưng") || errorLow.contains("ngọng");
            }
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

    /**
     * Lưu kết quả cuối bài entry test, cập nhật account, unlock level và tạo custom learning path.
     *
     * @param email       email học viên (từ JWT)
     * @param stepResults danh sách kết quả từng bước analyze (accuracy, regionCategory, isRegional, …)
     * @return {@link EntryTestResult} đã persist
     * @throws BadRequestException khi {@code stepResults} null hoặc rỗng (BUG-003)
     * @throws ResourceNotFoundException khi không tìm thấy account
     *
     * <p><b>Note:</b> Đã xử lý quăng lỗi 400 khi mảng {@code stepResults} rỗng để tránh chia cho 0
     * và sập server 500 (BUG-003).</p>
     */
    @Transactional
    public EntryTestResult saveFinalResult(String email, List<Map<String, Object>> stepResults) {
        if (stepResults == null || stepResults.isEmpty()) {
            throw new BadRequestException("Danh sách kết quả bước không được rỗng");
        }

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        double totalAccuracy = 0;
        Map<RegionCode, Integer> regionErrorCount = new HashMap<>();
        regionErrorCount.put(RegionCode.NORTH, 0);
        regionErrorCount.put(RegionCode.CENTRAL, 0);
        regionErrorCount.put(RegionCode.SOUTH, 0);

        for (Map<String, Object> res : stepResults) {
            Object accObj = res.getOrDefault("accuracy", 0);
            double accuracy = 0;
            if (accObj instanceof Number num) {
                accuracy = num.doubleValue();
            } else if (accObj instanceof String s) {
                try { accuracy = Double.parseDouble(s); } catch (Exception e) {}
            }
            totalAccuracy += accuracy;

            boolean isRegional = Boolean.TRUE.equals(res.get("isRegional"));
            if (isRegional) {
                Object catObj = res.get("regionCategory");
                String catStr = null;
                if (catObj instanceof String s) {
                    catStr = s.toUpperCase();
                } else if (catObj instanceof EntryTestRegionCategory catEnum) {
                    catStr = catEnum.name().toUpperCase();
                }

                if (catStr != null && !catStr.isBlank()) {
                    if (catStr.contains("NORTH"))
                        regionErrorCount.put(RegionCode.NORTH, regionErrorCount.get(RegionCode.NORTH) + 1);
                    else if (catStr.contains("CENTRAL"))
                        regionErrorCount.put(RegionCode.CENTRAL, regionErrorCount.get(RegionCode.CENTRAL) + 1);
                    else if (catStr.contains("SOUTH"))
                        regionErrorCount.put(RegionCode.SOUTH, regionErrorCount.get(RegionCode.SOUTH) + 1);
                }
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
                if (catObj instanceof String s && !s.isBlank()) {
                    try {
                        category = EntryTestRegionCategory.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid region category string for fallback: {}", s);
                    }
                } else if (catObj instanceof EntryTestRegionCategory) {
                    category = (EntryTestRegionCategory) catObj;
                }

                RegionCode rc = mapCategoryToRegionCode(category);
                Object accObj = res.getOrDefault("accuracy", 0);
                double accVal = 0;
                if (accObj instanceof Number num) {
                    accVal = num.doubleValue();
                } else if (accObj instanceof String s) {
                    try { accVal = Double.parseDouble(s); } catch (Exception e) {}
                }
                
                regionAccuracy.put(rc, regionAccuracy.get(rc) + accVal);
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

    private RegionCode mapCategoryToRegionCode(Object categoryObj) {
        if (categoryObj == null) return RegionCode.NORTH;
        String name = "";
        if (categoryObj instanceof String s) {
            name = s.toUpperCase();
        } else if (categoryObj instanceof EntryTestRegionCategory category) {
            name = category.name().toUpperCase();
        }

        if (name.contains("NORTH")) return RegionCode.NORTH;
        if (name.contains("CENTRAL")) return RegionCode.CENTRAL;
        if (name.contains("SOUTH")) return RegionCode.SOUTH;

        return RegionCode.NORTH;
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

    public void assignPersonalRoadmap(Account account, RegionCode region, EntryTestResult result, List<Map<String, Object>> stepResults) {
        // === Bước 1: Thu thập điểm từng category để phân tích lỗi ===
        Map<String, List<Integer>> categoryScores = new HashMap<>();
        Map<String, Integer> totalWrong = new HashMap<>();
        
        for (Map<String, Object> detail : stepResults) {
            Object regionCatObj = detail.get("regionCategory");
            Object accObj = detail.get("accuracy");
            
            if (regionCatObj == null || accObj == null) {
                log.warn("Missing data in step result: regionCategory={}, accuracy={}", regionCatObj, accObj);
                continue;
            }
            
            String regionCatStr = regionCatObj.toString();
            int accuracyValue = 0;
            if (accObj instanceof Number) {
                accuracyValue = ((Number) accObj).intValue();
            } else {
                try {
                    accuracyValue = Integer.parseInt(accObj.toString());
                } catch (Exception e) {
                    accuracyValue = 0;
                }
            }
            
            categoryScores.computeIfAbsent(regionCatStr, k -> new ArrayList<>()).add(accuracyValue);
            if (accuracyValue < 30) totalWrong.put(regionCatStr, totalWrong.getOrDefault(regionCatStr, 0) + 1);
        }

        Map<String, Double> categoryAccuracy = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : categoryScores.entrySet()) {
            double avg = entry.getValue().stream().mapToInt(i -> i).average().orElse(0.0);
            categoryAccuracy.put(entry.getKey(), avg);
        }

        // Sắp xếp category theo điểm tăng dần (lỗi nặng nhất lên đầu)
        List<String> sortedCategories = categoryAccuracy.keySet().stream()
                .sorted((c1, c2) -> Double.compare(categoryAccuracy.get(c1), categoryAccuracy.get(c2)))
                .collect(Collectors.toList());

        // === Bước 2: Xác định difficulty_level dựa trên OVERALL SCORE từ roadmap_rules ===
        double overallScore = result.getOverallScore();
        List<String> difficultiesToAssign = roadmapRuleService.getDifficultiesForScore(overallScore);
        log.info("[Roadmap] Overall score={}, difficulties từ roadmap_rules: {}", overallScore, difficultiesToAssign);

        // === Bước 3: Deactivate các lộ trình cũ của học viên ===
        List<CustomLearningPath> existingPaths = customLearningPathRepository.findByStudentIdAndIsActiveTrue(account.getId());
        if (!existingPaths.isEmpty()) {
            existingPaths.forEach(p -> p.setIsActive(false));
            customLearningPathRepository.saveAll(existingPaths);
            log.info("[Roadmap] Đã deactivate {} lộ trình cũ của học viên {}", existingPaths.size(), account.getEmail());
        }

        // === Bước 4: Tạo lộ trình mới ===
        CustomLearningPath customPath = CustomLearningPath.builder()
                .student(account)
                .title("Lộ trình Học cá nhân hóa")
                .description("Lộ trình được tạo tự động bởi AI dựa trên kết quả kiểm tra đầu vào.")
                .isAiGenerated(true)
                .isActive(true)
                .aiFeedback("Đang phân tích...")
                .targetLevel(region.name())
                .levels(new ArrayList<>())
                .build();
        
        customPath = customLearningPathRepository.save(customPath);

        // === Bước 5: Gán các bài học theo category lỗi + difficulties từ overall score ===
        int orderIndex = 1;
        for (String cat : sortedCategories) {
            if (difficultiesToAssign.isEmpty()) {
                log.warn("[Roadmap] Không có difficulty nào được tìm thấy cho overallScore={}", overallScore);
                continue;
            }

            String resolvedErrorTagId = findErrorTagUnitId(cat);
            log.info("[Roadmap] Xử lý category={} -> errorTagUnitId={}, difficulties={}", cat, resolvedErrorTagId, difficultiesToAssign);

            for (String diff : difficultiesToAssign) {
                List<LearningUnit> units;
                if (resolvedErrorTagId != null) {
                    units = learningUnitRepository.findByTypeAndErrorTagIgnoreCaseAndDifficultyLevelIgnoreCase("LEVEL", resolvedErrorTagId, diff);
                    if (units.isEmpty()) {
                        // Fallback dùng tên category gốc nếu UUID không khớp
                        units = learningUnitRepository.findByTypeAndErrorTagIgnoreCaseAndDifficultyLevelIgnoreCase("LEVEL", cat, diff);
                    }
                } else {
                    units = learningUnitRepository.findByTypeAndErrorTagIgnoreCaseAndDifficultyLevelIgnoreCase("LEVEL", cat, diff);
                }

                if (units.isEmpty() && resolvedErrorTagId != null) {
                    log.info("[Roadmap] Không tìm thấy unit với difficulty={}, fallback toàn bộ errorTagId={}", diff, resolvedErrorTagId);
                    units = learningUnitRepository.findByTypeAndErrorTagIgnoreCase("LEVEL", resolvedErrorTagId);
                }

                log.info("[Roadmap] Tìm thấy {} units cho category={} (errorTagId={}) difficulty={}", units.size(), cat, resolvedErrorTagId, diff);

                for (LearningUnit unit : units) {
                    customPath.addLevel(unit, orderIndex++);
                }
            }
        }
        customPath = customLearningPathRepository.save(customPath);

        // AI Feedback integration
        StringBuilder errorDetailsBuilder = new StringBuilder();
        if (categoryAccuracy.isEmpty()) {
            errorDetailsBuilder.append("Không có lỗi ngọng vùng miền nghiêm trọng. ");
        } else {
            for (String cat : sortedCategories) {
                errorDetailsBuilder.append("Lỗi ").append(cat).append(" với độ chính xác ")
                        .append(String.format("%.1f", categoryAccuracy.get(cat))).append("% (có ")
                        .append(totalWrong.getOrDefault(cat, 0)).append(" câu sai hoàn toàn); ");
            }
        }

        String defaultTemplate = "Học viên mắc các lỗi sau trong phát âm: {errorDetails} Hãy viết 1 đoạn 3-4 câu nhận xét ngắn gọn, cổ vũ học viên và khuyên học viên nên ưu tiên học lỗi nào trước (dựa trên % độ chính xác thấp nhất). Trả về kết quả dưới dạng JSON có trường 'reply'.";
        String template = systemConfigService.getValue("prompt.entry-test-feedback", defaultTemplate);
        String prompt = template.replace("{errorDetails}", errorDetailsBuilder.toString());

        try {
            Map<String, Object> aiResponse = aiService.chatWithGroq(prompt);
            String aiFeedback = "Bạn cần cố gắng luyện tập thêm!";
            if (aiResponse.containsKey("reply")) aiFeedback = (String) aiResponse.get("reply");
            else if (aiResponse.containsKey("feedback")) aiFeedback = (String) aiResponse.get("feedback");
            else if (aiResponse.containsKey("explanation")) aiFeedback = (String) aiResponse.get("explanation");
            
            customPath.setAiFeedback(aiFeedback);
            customLearningPathRepository.save(customPath);
        } catch (Exception e) {
            log.error("Error generating AI analysis for custom path", e);
            customPath.setAiFeedback("Hệ thống AI đang bận. Dựa trên kết quả bài test, bạn đã được phân bổ lộ trình học phù hợp với lỗi phát âm của mình.");
            customLearningPathRepository.save(customPath);
        }
    }

    /**
     * Tìm UUID của ERROR_TAG LearningUnit phù hợp với category từ entry test.
     * Cột error_tag trong LEVEL records lưu UUID của ERROR_TAG unit.
     * Dùng ký tự sorted để khớp: "NORTH_NL" → suffix "NL" ≡ "LN" trong tên "L - N".
     */
    private String findErrorTagUnitId(String categorySearch) {
        if (categorySearch == null) return null;

        // Trích xuất suffix sau "_" cuối (vd: "NORTH_NL" → "NL", "SOUTH_TRCH" → "TRCH")
        String suffix = categorySearch.contains("_")
                ? categorySearch.substring(categorySearch.lastIndexOf("_") + 1)
                : categorySearch;
        String normalizedSuffix = suffix.replaceAll("[^A-Za-z]", "").toUpperCase();
        char[] suffixChars = normalizedSuffix.toCharArray();
        java.util.Arrays.sort(suffixChars);
        String sortedSuffix = new String(suffixChars);

        log.info("[Roadmap] findErrorTagUnitId: category='{}', suffix='{}', sortedSuffix='{}'",
                categorySearch, normalizedSuffix, sortedSuffix);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        return learningUnitRepository.findTop1000ByType("ERROR_TAG").stream()
                .filter(lu -> {
                    // 1. Khớp theo tên unit (sort ký tự để xử lý NL↔LN)
                    if (lu == null || lu.getName() == null) return false;
                    String unitName = lu.getName().replaceAll("[^A-Za-z]", "").toUpperCase();
                    char[] unitChars = unitName.toCharArray();
                    java.util.Arrays.sort(unitChars);
                    String sortedUnit = new String(unitChars);
                    if (sortedSuffix.equals(sortedUnit)) {
                        log.info("[Roadmap] Khớp theo tên unit: '{}' (sorted: {})", lu.getName(), sortedUnit);
                        return true;
                    }
                    // 2. Khớp theo tag_code trong metadata
                    try {
                        if (lu.getMetadataJson() != null && !lu.getMetadataJson().isBlank()) {
                            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(lu.getMetadataJson());
                            if (node.has("tag_code")) {
                                String tagCode = node.get("tag_code").asText("").replaceAll("[^A-Za-z]", "").toUpperCase();
                                char[] tagChars = tagCode.toCharArray();
                                java.util.Arrays.sort(tagChars);
                                String sortedTag = new String(tagChars);
                                if (sortedSuffix.equals(sortedTag)) {
                                    log.info("[Roadmap] Khớp theo tag_code: '{}' (sorted: {})", tagCode, sortedTag);
                                    return true;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    return false;
                })
                .map(lu -> lu.getId().toString())
                .findFirst()
                .orElseGet(() -> {
                    log.warn("[Roadmap] Không tìm thấy ERROR_TAG nào khớp với category='{}'", categorySearch);
                    return null;
                });
    }

    private String findErrorTagName(String cat) {
        if (cat == null) return null;
        String categorySearch = cat.replaceAll("[_\\s-]", "").toUpperCase();

        return learningUnitRepository.findByType("ERROR_TAG").stream()
                .filter(lu -> {
                    if (lu == null || lu.getName() == null) return false;
                    String unitName = lu.getName().replaceAll("[_\\s-]", "").toUpperCase();
                    
                    return unitName.equals(categorySearch) || 
                           unitName.contains(categorySearch) || 
                           categorySearch.contains(unitName) ||
                           (categorySearch.endsWith("NL") && unitName.contains("LN")) ||
                           (categorySearch.endsWith("DGIR") && (unitName.contains("DGI") || unitName.contains("R")));
                })
                .map(LearningUnit::getName) // Return the NAME of the tag
                .findFirst()
                .orElse(null);
    }

    private long countTargetWords(String text, String errorCategory) {
        if (text == null || errorCategory == null)
            return 0;
        String[] words = text.toLowerCase().replaceAll("[^\\p{L}\\s]", "").split("\\s+");
        
        // Trích xuất các chữ cái đại diện từ mã lỗi (ví dụ: "L_N" -> ["l", "n"], "D_GI_R" -> ["d", "gi", "r"])
        List<String> startingChars = new ArrayList<>();
        for (String segment : errorCategory.toLowerCase().split("[_/-]")) {
            if (!segment.isBlank()) {
                startingChars.add(segment.trim());
            }
        }
        
        long count = 0;
        for (String w : words) {
            if (w.isEmpty())
                continue;
            for (String ch : startingChars) {
                if (w.startsWith(ch)) {
                    count++;
                    break;
                }
            }
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
                    if (lu == null || lu.getName() == null) return false;
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

    /**
     * Ánh xạ mã miền đã chuẩn hóa (NORTH/CENTRAL/SOUTH) sang category trong DB.
     *
     * @param regionKey giá trị đã {@code trim().toUpperCase()} và đã qua {@link #VALID_PLACEMENT_REGIONS}
     * @return tên category (NORTH_NL, CENTRAL_DGIR, SOUTH_TRCH)
     */
    private String mapPlacementRegionToCategory(String regionKey) {
        return switch (regionKey) {
            case "NORTH" -> EntryTestRegionCategory.NORTH_NL.name();
            case "CENTRAL" -> EntryTestRegionCategory.CENTRAL_DGIR.name();
            case "SOUTH" -> EntryTestRegionCategory.SOUTH_TRCH.name();
            default -> throw new IllegalStateException("regionKey must be validated before mapping: " + regionKey);
        };
    }

}
