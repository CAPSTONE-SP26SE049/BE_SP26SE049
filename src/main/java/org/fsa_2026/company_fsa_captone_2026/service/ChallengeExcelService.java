package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.DifficultyTag;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.QuizChallengeItemRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service tái sử dụng cho Import/Export Excel kho câu hỏi (Challenge Bank).
 * Hỗ trợ 4 kỹ năng: READING, LISTENING, WRITING, SPEAKING.
 * Mỗi kỹ năng có template Excel với các cột khác nhau phù hợp metadata.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeExcelService {

    private final ChallengeBankRepository challengeBankRepository;
    private final AccountRepository accountRepository;
    private final QuizChallengeItemRepository quizChallengeItemRepository;

    // ════════════════════════════════════════════════════════════════════
    //  COLUMN DEFINITIONS PER SKILL TYPE
    // ════════════════════════════════════════════════════════════════════

    /** Các cột chung cho tất cả kỹ năng */
    private static final String COL_CONTENT_TEXT  = "Tiêu đề / Yêu cầu";
    private static final String COL_DIFFICULTY    = "Độ khó (BEGINNER/INTERMEDIATE/ADVANCED)";
    private static final String COL_IS_GLOBAL     = "Dùng chung (true/false)";
    private static final String COL_REGION        = "Miền (BAC/TRUNG/NAM)";

    /** Cột riêng theo kỹ năng */
    private static final String COL_OPTIONS        = "Các lựa chọn (phân cách bằng |)";
    private static final String COL_CORRECT_ANSWER = "Đáp án chính xác";
    private static final String COL_HINT           = "Gợi ý";
    private static final String COL_IMAGE_URL      = "Hình ảnh URL"; // Not used for READING anymore, but kept for legacy/other
    private static final String COL_AUDIO_URL      = "Audio URL";
    private static final String COL_TRANSCRIPT     = "Transcript / Lời thoại";
    private static final String COL_SCRAMBLED      = "Các từ xáo trộn (phân cách bằng |)";
    private static final String COL_CORRECT_SENTENCE = "Câu hoàn chỉnh đúng";
    
    // New constants for READING
    private static final String COL_READING_WORDS = "Các từ trong câu (phân cách bằng |)";
    private static final String COL_READING_ERROR_INDEX = "Vị trí từ sai (bắt đầu từ 0)";
    private static final String COL_READING_CORRECT_WORD = "Từ viết đúng";

    /**
     * Trả về danh sách headers cho từng skill type.
     */
    private List<String> getHeaders(SkillType skillType) {
        List<String> headers = new ArrayList<>(List.of(COL_CONTENT_TEXT, COL_DIFFICULTY, COL_IS_GLOBAL, COL_REGION));

        switch (skillType) {
            case READING -> headers.addAll(List.of(COL_READING_WORDS, COL_READING_ERROR_INDEX, COL_READING_CORRECT_WORD, COL_HINT));
            case LISTENING -> headers.addAll(List.of(COL_AUDIO_URL, COL_OPTIONS, COL_CORRECT_ANSWER, COL_TRANSCRIPT));
            case WRITING -> headers.addAll(List.of(COL_SCRAMBLED, COL_CORRECT_SENTENCE, COL_HINT));
            case SPEAKING -> headers.addAll(List.of(COL_AUDIO_URL, COL_TRANSCRIPT, COL_HINT));
        }

        return headers;
    }

    // ════════════════════════════════════════════════════════════════════
    //  1. TEMPLATE DOWNLOAD
    // ════════════════════════════════════════════════════════════════════

    /**
     * Tạo file Excel mẫu (.xlsx) cho kỹ năng chỉ định, bao gồm 2 dòng dữ liệu mẫu.
     */
    public byte[] generateTemplate(SkillType skillType) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Template_" + skillType.name());

            List<String> headers = getHeaders(skillType);

            // === Header row (bold, blue background) ===
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // === Sample data rows ===
            List<List<String>> sampleData = getSampleData(skillType);
            for (int r = 0; r < sampleData.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> values = sampleData.get(r);
                for (int c = 0; c < values.size(); c++) {
                    row.createCell(c).setCellValue(values.get(c));
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                // Min width 15 chars
                if (sheet.getColumnWidth(i) < 15 * 256) {
                    sheet.setColumnWidth(i, 15 * 256);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo template Excel cho " + skillType, e);
        }
    }

    /**
     * Tạo file Excel mẫu (.xlsx) với nhiều sheet cho MIXED quiz (tất cả 4 kỹ năng).
     */
    public byte[] generateMixedTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            for (SkillType st : SkillType.values()) {
                Sheet sheet = workbook.createSheet("Template_" + st.name());
                List<String> headers = getHeaders(st);

                // Header style
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setColor(IndexedColors.WHITE.getIndex());
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderBottom(BorderStyle.THIN);

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                    cell.setCellStyle(headerStyle);
                }

                // Sample data
                List<List<String>> sampleData = getSampleData(st);
                for (int r = 0; r < sampleData.size(); r++) {
                    Row row = sheet.createRow(r + 1);
                    List<String> values = sampleData.get(r);
                    for (int c = 0; c < values.size(); c++) {
                        row.createCell(c).setCellValue(values.get(c));
                    }
                }

                // Auto-size
                for (int i = 0; i < headers.size(); i++) {
                    sheet.autoSizeColumn(i);
                    if (sheet.getColumnWidth(i) < 15 * 256) {
                        sheet.setColumnWidth(i, 15 * 256);
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo template Excel tổng hợp", e);
        }
    }

    private List<List<String>> getSampleData(SkillType skillType) {
        return switch (skillType) {
            case READING -> List.of(
                List.of("Tìm từ viết SAI trong câu (D/Đ/GI/R)", "BEGINNER", "true", "BAC",
                         "Con|lai|kia|chạy|lên|nương", "1", "nai", "Chú ý âm đầu L/N, lai -> nai"),
                List.of("Tìm từ viết SAI trong câu", "INTERMEDIATE", "true", "TRUNG",
                         "Trời|nạnh|quá|mọi|người|mặc|áo|ấm", "1", "lạnh", "L/N, nạnh -> lạnh")
            );
            case LISTENING -> List.of(
                List.of("Nghe và chọn từ đúng", "BEGINNER", "true", "BAC",
                         "https://example.com/audio1.mp3", "Lúa nếp|Lúa nết|Núa nếp", "Lúa nếp", "Lúa nếp là lúa nếp làng"),
                List.of("Nghe đoạn audio và chọn đáp án", "INTERMEDIATE", "true", "NAM",
                         "https://example.com/audio2.mp3", "Nón lá|Lón lá|Nón nà", "Nón lá", "Chiếc nón lá Việt Nam")
            );
            case WRITING -> List.of(
                List.of("Sắp xếp lại câu đúng", "BEGINNER", "true", "BAC",
                         "Nếp|Lúa|Làng|Là|Nếp|Lúa", "Lúa nếp là lúa nếp làng", "Gợi ý: câu tục ngữ"),
                List.of("Viết lại câu hoàn chỉnh", "INTERMEDIATE", "true", "TRUNG",
                         "Việt|Nam|Nón|Lá|Chiếc", "Chiếc nón lá Việt Nam", "")
            );
            case SPEAKING -> List.of(
                List.of("Đọc to câu sau", "BEGINNER", "true", "NAM",
                         "https://example.com/ref1.mp3", "Lúa nếp là lúa nếp làng", "Chú ý phân biệt N và L"),
                List.of("Phát âm câu sau", "INTERMEDIATE", "true", "BAC",
                         "https://example.com/ref2.mp3", "Con lợn nằm trong chuồng", "Chú ý âm đầu L")
            );
        };
    }

    // ════════════════════════════════════════════════════════════════════
    //  2. IMPORT
    // ════════════════════════════════════════════════════════════════════

    /**
     * Kết quả trả về sau khi import.
     */
    public record ImportResult(int successCount, int skipCount, int errorCount, List<String> messages) {}

    /**
     * Parse file Excel HOẶC CSV và import câu hỏi. Check trùng bằng contentText + skillType.
     */
    @Transactional
    public ImportResult importFromExcel(SkillType skillType, MultipartFile file) {
        // Lấy user hiện tại
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID createdBy = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"))
                .getId();

        String filename = file.getOriginalFilename();
        boolean isCsv = filename != null && filename.toLowerCase().endsWith(".csv");

        if (isCsv) {
            return importFromCsvInternal(skillType, file, createdBy, null);
        }

        // --- Excel logic (existing) ---
        List<String> headers = getHeaders(skillType);
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        List<String> messages = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("File Excel không có header row");
            }

            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    colMap.put(getCellString(cell).trim(), i);
                }
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    String contentText = getCellValue(row, colMap, COL_CONTENT_TEXT);
                    if (contentText == null || contentText.isBlank()) {
                        continue;
                    }

                    String diffStr = getCellValue(row, colMap, COL_DIFFICULTY);
                    DifficultyTag difficulty = parseDifficulty(diffStr);
                    String globalStr = getCellValue(row, colMap, COL_IS_GLOBAL);
                    boolean isGlobal = globalStr == null || globalStr.isBlank() || "true".equalsIgnoreCase(globalStr.trim());
                    String regionStr = getCellValue(row, colMap, COL_REGION);
                    String region = parseRegion(regionStr);

                    Map<String, Object> metadata = buildMetadata(skillType, row, colMap);

                    ChallengeBank challenge = ChallengeBank.builder()
                            .contentText(contentText)
                            .skillType(skillType)
                            .difficultyTag(difficulty)
                            .isGlobal(isGlobal)
                            .region(region)
                            .metadataJson(metadata)
                            .createdBy(createdBy)
                            .build();

                    challengeBankRepository.save(challenge);
                    successCount++;

                } catch (Exception e) {
                    errorCount++;
                    messages.add("Dòng " + (r + 1) + ": Lỗi — " + e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file Excel: " + e.getMessage(), e);
        }

        messages.add(0, String.format("Import hoàn tất: %d thành công, %d bỏ qua (trùng), %d lỗi",
                successCount, skipCount, errorCount));

        return new ImportResult(successCount, skipCount, errorCount, messages);
    }

    /**
     * Import câu hỏi từ Excel/CSV vào Challenge Bank VÀ tự động gán vào Quiz.
     * Nếu skillType = null → đọc tất cả sheet, mỗi sheet là 1 kỹ năng (MIXED template).
     */
    @Transactional
    public ImportResult importFromExcelToQuiz(SkillType skillType, MultipartFile file, UUID quizId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID createdBy = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"))
                .getId();

        String filename = file.getOriginalFilename();
        boolean isCsv = filename != null && filename.toLowerCase().endsWith(".csv");

        if (isCsv) {
            // CSV vào quiz: dùng skillType truyền vào, nếu null thì mặc định READING
            SkillType csvSkill = skillType != null ? skillType : SkillType.READING;
            return importFromCsvInternal(csvSkill, file, createdBy, quizId);
        }

        // --- Excel logic (existing) ---
        int totalSuccess = 0;
        int totalSkip = 0;
        int totalError = 0;
        List<String> allMessages = new ArrayList<>();
        List<UUID> newChallengeIds = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
                Sheet sheet = workbook.getSheetAt(sheetIdx);
                String sheetName = sheet.getSheetName().toUpperCase();

                SkillType st;
                if (skillType != null) {
                    st = skillType;
                } else {
                    st = detectSkillTypeFromSheetName(sheetName);
                    if (st == null) {
                        allMessages.add("Sheet '" + sheet.getSheetName() + "': Bỏ qua — không nhận diện được kỹ năng");
                        continue;
                    }
                }

                allMessages.add("── Sheet: " + st.name() + " ──");

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) {
                    allMessages.add("Sheet '" + st.name() + "': Không có header row");
                    continue;
                }

                Map<String, Integer> colMap = new HashMap<>();
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null) {
                        colMap.put(getCellString(cell).trim(), i);
                    }
                }

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    try {
                        String contentText = getCellValue(row, colMap, COL_CONTENT_TEXT);
                        if (contentText == null || contentText.isBlank()) continue;

                        String diffStr = getCellValue(row, colMap, COL_DIFFICULTY);
                        DifficultyTag difficulty = parseDifficulty(diffStr);
                        String globalStr = getCellValue(row, colMap, COL_IS_GLOBAL);
                        boolean isGlobal = globalStr == null || globalStr.isBlank() || "true".equalsIgnoreCase(globalStr.trim());
                        String regionStr = getCellValue(row, colMap, COL_REGION);
                        String region = parseRegion(regionStr);
                        Map<String, Object> metadata = buildMetadata(st, row, colMap);

                        ChallengeBank challenge = ChallengeBank.builder()
                                .contentText(contentText)
                                .skillType(st)
                                .difficultyTag(difficulty)
                                .isGlobal(isGlobal)
                                .region(region)
                                .metadataJson(metadata)
                                .createdBy(createdBy)
                                .build();

                        ChallengeBank saved = challengeBankRepository.save(challenge);
                        newChallengeIds.add(saved.getId());
                        totalSuccess++;

                    } catch (Exception e) {
                        totalError++;
                        allMessages.add("  Dòng " + (r + 1) + ": Lỗi — " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file Excel: " + e.getMessage(), e);
        }

        // Auto-assign all new challenges to quiz
        assignChallengestoQuiz(newChallengeIds, quizId, allMessages);

        allMessages.add(0, String.format("Import hoàn tất: %d thành công, %d bỏ qua (trùng), %d lỗi",
                totalSuccess, totalSkip, totalError));

        return new ImportResult(totalSuccess, totalSkip, totalError, allMessages);
    }

    // ════════════════════════════════════════════════════════════════════
    //  CSV IMPORT INTERNAL
    // ════════════════════════════════════════════════════════════════════

    /**
     * Parse file CSV và import câu hỏi. Hỗ trợ cả import vào bank lẫn import vào quiz.
     * Nếu quizId != null thì tự động gán câu hỏi vào quiz.
     */
    private ImportResult importFromCsvInternal(SkillType skillType, MultipartFile file, UUID createdBy, UUID quizId) {
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;
        List<String> messages = new ArrayList<>();
        List<UUID> newChallengeIds = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Read header line
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new RuntimeException("File CSV không có header");
            }

            // Remove BOM if present
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String[] headerCols = headerLine.split(",", -1);
            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headerCols.length; i++) {
                colMap.put(headerCols[i].trim(), i);
            }

            // Read data lines
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;

                try {
                    String[] cols = line.split(",", -1);

                    String contentText = getCsvValue(cols, colMap, COL_CONTENT_TEXT);
                    if (contentText == null || contentText.isBlank()) continue;

                    String diffStr = getCsvValue(cols, colMap, COL_DIFFICULTY);
                    DifficultyTag difficulty = parseDifficulty(diffStr);
                    String globalStr = getCsvValue(cols, colMap, COL_IS_GLOBAL);
                    boolean isGlobal = globalStr == null || globalStr.isBlank() || "true".equalsIgnoreCase(globalStr.trim());
                    String regionStr = getCsvValue(cols, colMap, COL_REGION);
                    String region = parseRegion(regionStr);

                    // Build metadata from CSV columns
                    Map<String, Object> metadata = buildMetadataFromCsv(skillType, cols, colMap);

                    ChallengeBank challenge = ChallengeBank.builder()
                            .contentText(contentText)
                            .skillType(skillType)
                            .difficultyTag(difficulty)
                            .isGlobal(isGlobal)
                            .region(region)
                            .metadataJson(metadata)
                            .createdBy(createdBy)
                            .build();

                    ChallengeBank saved = challengeBankRepository.save(challenge);
                    newChallengeIds.add(saved.getId());
                    successCount++;

                } catch (Exception e) {
                    errorCount++;
                    messages.add("Dòng " + lineNum + ": Lỗi — " + e.getMessage());
                }
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file CSV: " + e.getMessage(), e);
        }

        // Auto-assign to quiz if quizId is provided
        if (quizId != null) {
            assignChallengestoQuiz(newChallengeIds, quizId, messages);
        }

        messages.add(0, String.format("Import hoàn tất: %d thành công, %d bỏ qua (trùng), %d lỗi",
                successCount, skipCount, errorCount));

        return new ImportResult(successCount, skipCount, errorCount, messages);
    }

    /** Helper: Gán danh sách challenges vào quiz */
    private void assignChallengestoQuiz(List<UUID> challengeIds, UUID quizId, List<String> messages) {
        if (challengeIds.isEmpty()) return;

        List<QuizChallengeItem> existingItems = quizChallengeItemRepository.findByQuizIdOrderByOrderIndex(quizId);
        int maxOrder = existingItems.stream().mapToInt(i -> i.getOrderIndex() != null ? i.getOrderIndex() : 0).max().orElse(0);

        List<QuizChallengeItem> newItems = new ArrayList<>();
        for (int i = 0; i < challengeIds.size(); i++) {
            UUID cid = challengeIds.get(i);
            newItems.add(QuizChallengeItem.builder()
                    .quizId(quizId)
                    .challengeBankId(cid)
                    .challengeId(cid)
                    .orderIndex(maxOrder + i + 1)
                    .build());
        }
        quizChallengeItemRepository.saveAll(newItems);
        messages.add(String.format("→ Đã gán %d câu hỏi mới vào quiz", challengeIds.size()));
    }

    /** Lấy giá trị từ mảng CSV theo header map */
    private String getCsvValue(String[] cols, Map<String, Integer> colMap, String columnName) {
        Integer idx = colMap.get(columnName);
        if (idx == null || idx >= cols.length) return "";
        return cols[idx].trim();
    }

    /** Build metadata từ CSV row (tương tự buildMetadata nhưng dùng String[] thay vì Row) */
    private Map<String, Object> buildMetadataFromCsv(SkillType skillType, String[] cols, Map<String, Integer> colMap) {
        Map<String, Object> meta = new LinkedHashMap<>();

        switch (skillType) {
            case READING -> {
                meta.put("words", splitPipe(getCsvValue(cols, colMap, COL_READING_WORDS)));
                String errorIndexStr = getCsvValue(cols, colMap, COL_READING_ERROR_INDEX);
                int errorIndex = 0;
                try {
                    if (errorIndexStr != null && !errorIndexStr.isBlank()) {
                        errorIndex = (int) Double.parseDouble(errorIndexStr.trim());
                    }
                } catch (NumberFormatException ignored) {}
                meta.put("error_index", errorIndex);
                meta.put("correct_word", getCsvValue(cols, colMap, COL_READING_CORRECT_WORD));
                meta.put("hint", getCsvValue(cols, colMap, COL_HINT));
            }
            case LISTENING -> {
                meta.put("audioUrl", getCsvValue(cols, colMap, COL_AUDIO_URL));
                meta.put("options", splitPipe(getCsvValue(cols, colMap, COL_OPTIONS)));
                meta.put("correctAnswer", getCsvValue(cols, colMap, COL_CORRECT_ANSWER));
                meta.put("transcript", getCsvValue(cols, colMap, COL_TRANSCRIPT));
            }
            case WRITING -> {
                meta.put("scrambledWords", splitPipe(getCsvValue(cols, colMap, COL_SCRAMBLED)));
                meta.put("correctSentence", getCsvValue(cols, colMap, COL_CORRECT_SENTENCE));
                meta.put("hint", getCsvValue(cols, colMap, COL_HINT));
            }
            case SPEAKING -> {
                meta.put("audioUrl", getCsvValue(cols, colMap, COL_AUDIO_URL));
                meta.put("transcript", getCsvValue(cols, colMap, COL_TRANSCRIPT));
                meta.put("hint", getCsvValue(cols, colMap, COL_HINT));
            }
        }

        return meta;
    }

    private SkillType detectSkillTypeFromSheetName(String sheetName) {
        if (sheetName.contains("READING")) return SkillType.READING;
        if (sheetName.contains("LISTENING")) return SkillType.LISTENING;
        if (sheetName.contains("WRITING")) return SkillType.WRITING;
        if (sheetName.contains("SPEAKING")) return SkillType.SPEAKING;
        return null;
    }

    private Map<String, Object> buildMetadata(SkillType skillType, Row row, Map<String, Integer> colMap) {
        Map<String, Object> meta = new LinkedHashMap<>();

        switch (skillType) {
            case READING -> {
                // Parse READING as Godot game format: words, error_index, correct_word, hint
                meta.put("words", splitPipe(getCellValue(row, colMap, COL_READING_WORDS)));
                
                String errorIndexStr = getCellValue(row, colMap, COL_READING_ERROR_INDEX);
                int errorIndex = 0;
                try {
                    if (errorIndexStr != null && !errorIndexStr.isBlank()) {
                        errorIndex = (int) Double.parseDouble(errorIndexStr.trim());
                    }
                } catch (NumberFormatException ignored) {}
                meta.put("error_index", errorIndex);
                
                meta.put("correct_word", getCellValue(row, colMap, COL_READING_CORRECT_WORD));
                meta.put("hint", getCellValue(row, colMap, COL_HINT));
            }
            case LISTENING -> {
                meta.put("audioUrl", getCellValue(row, colMap, COL_AUDIO_URL));
                meta.put("options", splitPipe(getCellValue(row, colMap, COL_OPTIONS)));
                meta.put("correctAnswer", getCellValue(row, colMap, COL_CORRECT_ANSWER));
                meta.put("transcript", getCellValue(row, colMap, COL_TRANSCRIPT));
            }
            case WRITING -> {
                meta.put("scrambledWords", splitPipe(getCellValue(row, colMap, COL_SCRAMBLED)));
                meta.put("correctSentence", getCellValue(row, colMap, COL_CORRECT_SENTENCE));
                meta.put("hint", getCellValue(row, colMap, COL_HINT));
            }
            case SPEAKING -> {
                meta.put("audioUrl", getCellValue(row, colMap, COL_AUDIO_URL));
                meta.put("transcript", getCellValue(row, colMap, COL_TRANSCRIPT));
                meta.put("hint", getCellValue(row, colMap, COL_HINT));
            }
        }

        return meta;
    }

    // ════════════════════════════════════════════════════════════════════
    //  3. EXPORT
    // ════════════════════════════════════════════════════════════════════

    /**
     * Export tất cả (hoặc filter theo skillType) ra file Excel.
     */
    public byte[] exportToExcel(SkillType skillType) {
        List<ChallengeBank> data = (skillType != null)
                ? challengeBankRepository.findBySkillType(skillType)
                : challengeBankRepository.findAll();

        // Nếu có filter, dùng header của skill đó; nếu không, dùng READING làm mặc định + thêm cột skillType
        boolean mixed = (skillType == null);

        try (Workbook workbook = new XSSFWorkbook()) {
            if (mixed) {
                // Export tất cả — nhóm theo sheet
                for (SkillType st : SkillType.values()) {
                    List<ChallengeBank> filtered = data.stream()
                            .filter(c -> c.getSkillType() == st)
                            .toList();
                    if (!filtered.isEmpty()) {
                        writeSheet(workbook, st.name(), st, filtered);
                    }
                }
                // Nếu không có dữ liệu nào, tạo sheet trống
                if (workbook.getNumberOfSheets() == 0) {
                    workbook.createSheet("Empty");
                }
            } else {
                writeSheet(workbook, skillType.name(), skillType, data);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Không thể export Excel: " + e.getMessage(), e);
        }
    }

    private void writeSheet(Workbook workbook, String sheetName, SkillType skillType, List<ChallengeBank> data) {
        Sheet sheet = workbook.createSheet(sheetName);
        List<String> headers = getHeaders(skillType);

        // Header row
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (int r = 0; r < data.size(); r++) {
            Row row = sheet.createRow(r + 1);
            ChallengeBank cb = data.get(r);
            Map<String, Object> meta = cb.getMetadataJson() != null ? cb.getMetadataJson() : Map.of();

            // Common columns
            row.createCell(0).setCellValue(cb.getContentText());
            row.createCell(1).setCellValue(cb.getDifficultyTag() != null ? cb.getDifficultyTag().name() : "");
            row.createCell(2).setCellValue(cb.getIsGlobal() != null ? cb.getIsGlobal().toString() : "true");
            row.createCell(3).setCellValue(cb.getRegion() != null ? cb.getRegion() : "BAC");

            // Skill-specific columns
            int col = 4;
            switch (skillType) {
                case READING -> {
                    row.createCell(col++).setCellValue(joinList(meta.get("words")));
                    
                    Object errIdx = meta.get("error_index");
                    String errIdxStr = errIdx != null ? String.valueOf(errIdx) : "";
                    row.createCell(col++).setCellValue(errIdxStr);
                    
                    row.createCell(col++).setCellValue(str(meta.get("correct_word")));
                    row.createCell(col).setCellValue(str(meta.get("hint")));
                }
                case LISTENING -> {
                    row.createCell(col++).setCellValue(str(meta.get("audioUrl")));
                    row.createCell(col++).setCellValue(joinList(meta.get("options")));
                    row.createCell(col++).setCellValue(str(meta.get("correctAnswer")));
                    row.createCell(col).setCellValue(str(meta.get("transcript")));
                }
                case WRITING -> {
                    row.createCell(col++).setCellValue(joinList(meta.get("scrambledWords")));
                    row.createCell(col++).setCellValue(str(meta.get("correctSentence")));
                    row.createCell(col).setCellValue(str(meta.get("hint")));
                }
                case SPEAKING -> {
                    row.createCell(col++).setCellValue(str(meta.get("audioUrl")));
                    row.createCell(col++).setCellValue(str(meta.get("transcript")));
                    row.createCell(col).setCellValue(str(meta.get("hint")));
                }
            }
        }

        // Auto-size
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  UTILS
    // ════════════════════════════════════════════════════════════════════

    private String getCellValue(Row row, Map<String, Integer> colMap, String columnName) {
        Integer idx = colMap.get(columnName);
        if (idx == null) return "";
        Cell cell = row.getCell(idx);
        return cell != null ? getCellString(cell) : "";
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private List<String> splitPipe(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String joinList(Object value) {
        if (value instanceof List<?> list) {
            return String.join("|", list.stream().map(Object::toString).toList());
        }
        return value != null ? value.toString() : "";
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
    }

    private DifficultyTag parseDifficulty(String s) {
        if (s == null || s.isBlank()) return DifficultyTag.BEGINNER;
        try {
            return DifficultyTag.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DifficultyTag.BEGINNER;
        }
    }

    private String parseRegion(String s) {
        if (s == null || s.isBlank()) return "BAC";
        String upper = s.trim().toUpperCase();
        return switch (upper) {
            case "BAC", "BẮC", "NORTH" -> "BAC";
            case "TRUNG", "CENTRAL" -> "TRUNG";
            case "NAM", "SOUTH" -> "NAM";
            default -> "BAC";
        };
    }
}
