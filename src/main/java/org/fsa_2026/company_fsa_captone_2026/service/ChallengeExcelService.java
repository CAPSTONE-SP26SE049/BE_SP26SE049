package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.QuizChallengeItem;
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
    private static final String COL_REGION        = "Region (NORTH/CENTRAL/SOUTH)";

    /** Cột riêng theo kỹ năng */
    private static final String COL_OPTIONS        = "Các lựa chọn (phân cách bằng |)";
    private static final String COL_CORRECT_ANSWER = "Đáp án chính xác";
    private static final String COL_HINT           = "Gợi ý";
    // private static final String COL_IMAGE_URL      = "Hình ảnh URL"; // Not used for READING anymore, but kept for legacy/other
    private static final String COL_AUDIO_URL      = "Audio URL";
    private static final String COL_TRANSCRIPT     = "Transcript / Lời thoại";
    private static final String COL_WRITING_BLANK_SENTENCE = "Nội dung câu đố (với ký hiệu _ )";
    private static final String COL_WRITING_CORRECT_ANSWER = "Đáp án đúng (Correct Answer)";
    private static final String COL_WRITING_ALTERNATIVES   = "Đáp án chấp nhận khác (Alternative)";

    // LISTENING — 4 cột đáp án riêng (khớp với form trên web)
    private static final String COL_LISTENING_OPT1    = "Đáp án 1";
    private static final String COL_LISTENING_OPT2    = "Đáp án 2";
    private static final String COL_LISTENING_OPT3    = "Đáp án 3";
    private static final String COL_LISTENING_OPT4    = "Đáp án 4";
    private static final String COL_LISTENING_CORRECT = "Đáp án đúng";
    
    // New constants for READING
    private static final String COL_READING_SENTENCE = "Câu chứa lỗi sai (Sentence with Error)";
    private static final String COL_READING_WRONG_WORD = "Từ bị sai (Wrong Word)";
    private static final String COL_READING_CORRECT_WORD = "Từ viết đúng (Correct Word)";

    /**
     * Trả về danh sách headers cho từng skill type.
     */
    private List<String> getHeaders(SkillType skillType) {
        List<String> headers = new ArrayList<>(List.of(COL_CONTENT_TEXT, COL_REGION));

        switch (skillType) {
            case READING -> headers.addAll(List.of(COL_READING_SENTENCE, COL_READING_WRONG_WORD, COL_READING_CORRECT_WORD, COL_HINT));
            case LISTENING -> headers.addAll(List.of(COL_LISTENING_OPT1, COL_LISTENING_OPT2, COL_LISTENING_OPT3, COL_LISTENING_OPT4, COL_LISTENING_CORRECT, COL_TRANSCRIPT));
            case WRITING -> headers.addAll(List.of(COL_WRITING_BLANK_SENTENCE, COL_WRITING_CORRECT_ANSWER, COL_WRITING_ALTERNATIVES, COL_HINT));
            case SPEAKING, ENTRY_TEST -> headers.addAll(List.of(COL_TRANSCRIPT, COL_HINT));
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
                List.of("Tìm lỗi sai L/N trong câu", "NORTH",
                         "Con trâu đang ăn cỏ trên lồng.", "lồng.", "đồng", "Cánh đồng rộng lớn."),
                List.of("Tìm từ viết SAI trong câu", "CENTRAL",
                         "Trời nạnh quá mọi người mặc áo ấm", "nạnh", "lạnh", "L/N, nạnh -> lạnh")
            );
            case LISTENING -> List.of(
                List.of("Nghe và chọn từ đúng", "SOUTH",
                         "Lúa nếp", "Lúa nết", "Núa nếp", "Lúa tẻ", "Lúa nếp", "Lúa nếp là lúa nếp làng"),
                List.of("Nghe và chọn từ đúng", "SOUTH",
                         "Nón lá", "Lón lá", "Nón nà", "Nóm lá", "Nón lá", "Chiếc nón lá Việt Nam")
            );
            case WRITING -> List.of(
                List.of("Chọn từ đúng chính tả để điền vào chỗ trống", "NORTH",
                         "Lúa _ là lúa nếp làng", "nếp", "nếp cái,nếp thơm", "Ngược lại with nếp là tẻ"),
                List.of("Điền từ vào chỗ trống", "CENTRAL",
                         "Chiếc nón _ Việt Nam", "lá", "", "Làm từ lá cọ")
            );
            case SPEAKING -> List.of(
                List.of("Đọc to câu sau", "SOUTH",
                         "Lúa nếp là lúa nếp làng", "Chú ý phân biệt N và L"),
                List.of("Phát âm câu sau", "NORTH",
                         "Con lợn nằm trong chuồng", "Chú ý âm đầu L")
            );
            case ENTRY_TEST -> List.of(
                List.of("Vui lòng đọc câu sau để đánh giá giọng đọc của bạn", "NORTH",
                         "Lúa nếp là lúa nếp làng", "Hãy đọc chậm và rõ ràng"),
                List.of("Vui lòng đọc câu sau", "CENTRAL",
                         "Trời nắng chang chang vườn hoa vẫy gọi", "Chú ý âm sắc")
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
                    String raw = getCellString(cell).trim();
                    colMap.put(raw, i);
                    String normalized = raw.replaceAll("[\\s/()\\[\\]{}]+", " ").trim();
                    colMap.putIfAbsent(normalized, i);
                }
            }
            // Alias fallback for common header name variants
            colMap.putIfAbsent(COL_CONTENT_TEXT, colMap.getOrDefault("Tiêu đề bài tập / Yêu cầu",
                    colMap.getOrDefault("Tiêu đề", colMap.get("Yêu cầu"))));
            colMap.putIfAbsent(COL_REGION, colMap.getOrDefault("Vùng miền",
                    colMap.getOrDefault("Region", colMap.get("Miền"))));
            colMap.values().removeIf(v -> v == null);
            log.info("[Excel Import-Bank] headers: {}", colMap.keySet());

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                try {
                    String contentText = getCellValue(row, colMap, COL_CONTENT_TEXT);
                    if (contentText == null || contentText.isBlank()) {
                        continue;
                    }

                    String regionStr = getCellValue(row, colMap, COL_REGION);
                    String region = parseRegion(regionStr);

                    Map<String, Object> metadata = buildMetadata(skillType, row, colMap);

                    ChallengeBank challenge = ChallengeBank.builder()
                            .contentText(contentText)
                            .skillType(skillType)
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
                        String raw = getCellString(cell).trim();
                        colMap.put(raw, i);
                        // Also normalize: remove ()/{}/[] details for fuzzy match
                        String normalized = raw.replaceAll("[\\s/()\\[\\]{}]+", " ").trim();
                        colMap.putIfAbsent(normalized, i);
                    }
                }

                // Also register key alias variants so old files still work
                colMap.putIfAbsent(COL_CONTENT_TEXT, colMap.getOrDefault("Tiêu đề bài tập / Yêu cầu",
                        colMap.getOrDefault("Tiêu đề", colMap.get("Yêu cầu"))));
                colMap.putIfAbsent(COL_REGION, colMap.getOrDefault("Vùng miền",
                        colMap.getOrDefault("Region", colMap.get("Miền"))));
                colMap.values().removeIf(v -> v == null);

                // Debug: log detected columns
                log.info("[Excel Import] Sheet '{}' headers: {}", st.name(), colMap.keySet());

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    try {
                        String contentText = getCellValue(row, colMap, COL_CONTENT_TEXT);
                        if (contentText == null || contentText.isBlank()) continue;

                        String regionStr = getCellValue(row, colMap, COL_REGION);
                        String region = parseRegion(regionStr);
                        Map<String, Object> metadata = buildMetadata(st, row, colMap);

                        ChallengeBank challenge = ChallengeBank.builder()
                                .contentText(contentText)
                                .skillType(st)
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

                    String regionStr = getCsvValue(cols, colMap, COL_REGION);
                    String region = parseRegion(regionStr);

                    // Build metadata from CSV columns
                    Map<String, Object> metadata = buildMetadataFromCsv(skillType, cols, colMap);

                    ChallengeBank challenge = ChallengeBank.builder()
                            .contentText(contentText)
                            .skillType(skillType)
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
                String sentence = getCsvValue(cols, colMap, COL_READING_SENTENCE);
                String wrongWord = getCsvValue(cols, colMap, COL_READING_WRONG_WORD);
                List<String> words = sentence != null && !sentence.isBlank() ? Arrays.asList(sentence.trim().split("\\s+")) : new ArrayList<>();
                int errorIndex = 0;
                if (wrongWord != null && !wrongWord.isEmpty() && !words.isEmpty()) {
                    String cleanWrong = wrongWord.toLowerCase().replaceAll("[.,!?;:]", "");
                    for (int i = 0; i < words.size(); i++) {
                        if (words.get(i).toLowerCase().replaceAll("[.,!?;:]", "").equals(cleanWrong)) {
                            errorIndex = i;
                            break;
                        }
                    }
                }
                meta.put("words", words);
                meta.put("error_index", errorIndex);
                meta.put("correct_word", getCsvValue(cols, colMap, COL_READING_CORRECT_WORD));
                meta.put("hint", getCsvValue(cols, colMap, COL_HINT));
            }
            case LISTENING -> {
                String csvO1 = getCsvValue(cols, colMap, COL_LISTENING_OPT1);
                String csvO2 = getCsvValue(cols, colMap, COL_LISTENING_OPT2);
                String csvO3 = getCsvValue(cols, colMap, COL_LISTENING_OPT3);
                String csvO4 = getCsvValue(cols, colMap, COL_LISTENING_OPT4);
                if (csvO1.isBlank() && csvO2.isBlank()) {
                    meta.put("options", splitPipe(getCsvValue(cols, colMap, COL_OPTIONS)));
                    meta.put("correctAnswer", getCsvValue(cols, colMap, COL_CORRECT_ANSWER));
                } else {
                    java.util.List<String> csvOpts = new ArrayList<>();
                    if (!csvO1.isBlank()) csvOpts.add(csvO1);
                    if (!csvO2.isBlank()) csvOpts.add(csvO2);
                    if (!csvO3.isBlank()) csvOpts.add(csvO3);
                    if (!csvO4.isBlank()) csvOpts.add(csvO4);
                    meta.put("options", csvOpts);
                    meta.put("correctAnswer", getCsvValue(cols, colMap, COL_LISTENING_CORRECT));
                }
                meta.put("transcript", getCsvValue(cols, colMap, COL_TRANSCRIPT));
            }
            case WRITING -> {
                meta.put("blankSentence", getCsvValue(cols, colMap, COL_WRITING_BLANK_SENTENCE));
                meta.put("correctAnswer", getCsvValue(cols, colMap, COL_WRITING_CORRECT_ANSWER));
                String altStr = getCsvValue(cols, colMap, COL_WRITING_ALTERNATIVES);
                meta.put("alternatives", altStr != null && !altStr.isBlank() ? Arrays.stream(altStr.split("[,;]+")).map(String::trim).filter(s -> !s.isEmpty()).toList() : List.of());
                meta.put("hint", getCsvValue(cols, colMap, COL_HINT));
            }
            case SPEAKING, ENTRY_TEST -> {
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
        if (sheetName.contains("ENTRY_TEST")) return SkillType.ENTRY_TEST;
        return null;
    }

    private Map<String, Object> buildMetadata(SkillType skillType, Row row, Map<String, Integer> colMap) {
        Map<String, Object> meta = new LinkedHashMap<>();

        switch (skillType) {
            case READING -> {
                String sentence = getCellValue(row, colMap, COL_READING_SENTENCE);
                String wrongWord = getCellValue(row, colMap, COL_READING_WRONG_WORD);
                List<String> words = sentence != null && !sentence.isBlank() ? Arrays.asList(sentence.trim().split("\\s+")) : new ArrayList<>();
                int errorIndex = 0;
                if (wrongWord != null && !wrongWord.isEmpty() && !words.isEmpty()) {
                    String cleanWrong = wrongWord.toLowerCase().replaceAll("[.,!?;:]", "");
                    for (int i = 0; i < words.size(); i++) {
                        if (words.get(i).toLowerCase().replaceAll("[.,!?;:]", "").equals(cleanWrong)) {
                            errorIndex = i;
                            break;
                        }
                    }
                }
                meta.put("words", words);
                meta.put("error_index", errorIndex);
                meta.put("correct_word", getCellValue(row, colMap, COL_READING_CORRECT_WORD));
                meta.put("hint", getCellValue(row, colMap, COL_HINT));
            }
            case LISTENING -> {
                String xO1 = getCellValue(row, colMap, COL_LISTENING_OPT1);
                String xO2 = getCellValue(row, colMap, COL_LISTENING_OPT2);
                String xO3 = getCellValue(row, colMap, COL_LISTENING_OPT3);
                String xO4 = getCellValue(row, colMap, COL_LISTENING_OPT4);
                if ((xO1 == null || xO1.isBlank()) && (xO2 == null || xO2.isBlank())) {
                    meta.put("options", splitPipe(getCellValue(row, colMap, COL_OPTIONS)));
                    meta.put("correctAnswer", getCellValue(row, colMap, COL_CORRECT_ANSWER));
                } else {
                    java.util.List<String> xOpts = new ArrayList<>();
                    if (xO1 != null && !xO1.isBlank()) xOpts.add(xO1);
                    if (xO2 != null && !xO2.isBlank()) xOpts.add(xO2);
                    if (xO3 != null && !xO3.isBlank()) xOpts.add(xO3);
                    if (xO4 != null && !xO4.isBlank()) xOpts.add(xO4);
                    meta.put("options", xOpts);
                    meta.put("correctAnswer", getCellValue(row, colMap, COL_LISTENING_CORRECT));
                }
                meta.put("transcript", getCellValue(row, colMap, COL_TRANSCRIPT));
            }
            case WRITING -> {
                meta.put("blankSentence", getCellValue(row, colMap, COL_WRITING_BLANK_SENTENCE));
                meta.put("correctAnswer", getCellValue(row, colMap, COL_WRITING_CORRECT_ANSWER));
                String altStr = getCellValue(row, colMap, COL_WRITING_ALTERNATIVES);
                meta.put("alternatives", altStr != null && !altStr.isBlank() ? Arrays.stream(altStr.split("[,;]+")).map(String::trim).filter(s -> !s.isEmpty()).toList() : List.of());
                meta.put("hint", getCellValue(row, colMap, COL_HINT));
            }
            case SPEAKING, ENTRY_TEST -> {
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
                writeSheet(workbook, skillType != null ? skillType.name() : "Data", skillType, data);
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
            row.createCell(1).setCellValue(mapRegionToEnglish(cb.getRegion()));

            // Skill-specific columns
            int col = 2;
            switch (skillType) {
                case READING -> {
                    Object wordsObj = meta.get("words");
                    String sentence = wordsObj instanceof List<?> list ? String.join(" ", list.stream().map(Object::toString).toList()) : str(wordsObj);
                    row.createCell(col++).setCellValue(sentence);
                    
                    Object errIdxObj = meta.get("error_index");
                    String wrongWord = "";
                    if (wordsObj instanceof List<?> list) {
                        try {
                            int idx = -1;
                            if (errIdxObj instanceof Number n) idx = n.intValue();
                            else if (errIdxObj instanceof String s) idx = Integer.parseInt(s);
                            
                            if (idx >= 0 && idx < list.size()) {
                                wrongWord = String.valueOf(list.get(idx));
                            }
                        } catch (Exception ignore) {}
                    }
                    row.createCell(col++).setCellValue(wrongWord);
                    
                    row.createCell(col++).setCellValue(str(meta.get("correct_word")));
                    row.createCell(col).setCellValue(str(meta.get("hint")));
                }
                case LISTENING -> {
                    Object wo = meta.get("options");
                    java.util.List<?> wol = wo instanceof java.util.List<?> wl ? wl : java.util.List.of();
                    row.createCell(col++).setCellValue(wol.size() > 0 ? str(wol.get(0)) : "");
                    row.createCell(col++).setCellValue(wol.size() > 1 ? str(wol.get(1)) : "");
                    row.createCell(col++).setCellValue(wol.size() > 2 ? str(wol.get(2)) : "");
                    row.createCell(col++).setCellValue(wol.size() > 3 ? str(wol.get(3)) : "");
                    row.createCell(col++).setCellValue(str(meta.get("correctAnswer")));
                    row.createCell(col).setCellValue(str(meta.get("transcript")));
                }
                case WRITING -> {
                    row.createCell(col++).setCellValue(str(meta.get("blankSentence")));
                    row.createCell(col++).setCellValue(str(meta.get("correctAnswer")));
                    Object altObj = meta.get("alternatives");
                    String altStr = altObj instanceof List<?> list ? String.join(",", list.stream().map(Object::toString).toList()) : str(altObj);
                    row.createCell(col++).setCellValue(altStr);
                    row.createCell(col).setCellValue(str(meta.get("hint")));
                }
                case SPEAKING, ENTRY_TEST -> {
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


    private String parseRegion(String s) {
        if (s == null || s.isBlank()) return "BAC";
        String upper = s.trim().toUpperCase();
        return switch (upper) {
            case "NORTH", "NORTHERN", "BAC", "BẮC" -> "BAC";
            case "CENTRAL", "TRUNG" -> "TRUNG";
            case "SOUTH", "SOUTHERN", "NAM" -> "NAM";
            default -> "BAC";
        };
    }

    private String mapRegionToEnglish(String regionCode) {
        if (regionCode == null) return "NORTH";
        return switch (regionCode.toUpperCase()) {
            case "BAC" -> "NORTH";
            case "TRUNG" -> "CENTRAL";
            case "NAM" -> "SOUTH";
            default -> "NORTH";
        };
    }
}
