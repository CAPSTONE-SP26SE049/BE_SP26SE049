package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.fsa_2026.company_fsa_captone_2026.dto.ChallengeBankRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminChallengeBankExcelService {

    // Fixed columns
    private static final String COL_CONTENT_TEXT = "contentText";
    private static final String COL_SKILL_TYPE = "skillType";
    private static final String COL_REGION = "region";
    private static final String COL_HINT = "hint";

    // READING columns
    private static final String COL_SENTENCE_WITH_ERROR = "sentenceWithError";
    private static final String COL_WRONG_WORD = "wrongWord";
    private static final String COL_CORRECT_WORD = "correctWord";
    // LISTENING columns
    private static final String COL_AUDIO_URL = "audioUrl";
    private static final String COL_OPTIONS = "options";
    private static final String COL_CORRECT_ANSWER = "correctAnswer";
    private static final String COL_TRANSCRIPT = "transcript";
    // WRITING columns
    private static final String COL_SCRAMBLED_WORDS = "scrambledWords";
    private static final String COL_CORRECT_SENTENCE = "correctSentence";

    private final ChallengeBankService challengeBankService;
    private final ChallengeBankRepository challengeBankRepository;

    public record ImportResult(int successCount, int skipCount, int errorCount, List<String> messages) {
    }

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet chính của template — 1 sheet bao quát (cột cố định + cột linh hoạt)
            Sheet sheet = workbook.createSheet("ChallengeBank");
            List<String> headers = headers();

            // Tạo header + style như hiện tại (không thay đổi logic cũ)
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // =========================
            // Data Validation (Dropdown list) cho các cột Enum/Code
            // Áp dụng từ dòng 2 đến dòng 1000 (Excel row index 1..999)
            // =========================
            // Lưu ý quan trọng:
            // - Dropdown sẽ HIỂN THỊ tiếng Việt để user chọn cho dễ.
            // - Khi import, hệ thống sẽ map tiếng Việt -> enum/code nội bộ.
            // - Khi export, hệ thống sẽ map enum/code nội bộ -> tiếng Việt để ghi ra Excel.
            // Việc thêm validation không làm thay đổi header/styling hay sample rows bên
            // dưới.
            addDropdownValidation(sheet, headers, COL_SKILL_TYPE,
                    new String[] { "Đọc hiểu", "Nghe hiểu", "Viết", "Nói", "Kiểm tra đầu vào" });
            addDropdownValidation(sheet, headers, COL_REGION,
                    new String[] { "NORTH", "CENTRAL", "SOUTH" });

            // Sample rows (3 kỹ năng đại diện)
            Row r1 = sheet.createRow(1);
            setRow(r1, headers, Map.of(
                    COL_CONTENT_TEXT, "Chọn từ sai chính tả trong câu sau:",
                    COL_SKILL_TYPE, "Đọc hiểu",
                    COL_REGION, "NORTH",
                    COL_HINT, "Chú ý phân biệt âm đầu L and N.",
                    COL_SENTENCE_WITH_ERROR, "Con nợn đang ăn cỏ ngoài đồng.",
                    COL_WRONG_WORD, "nợn",
                    COL_CORRECT_WORD, "lợn"));

            Row r2 = sheet.createRow(2);
            setRow(r2, headers, Map.of(
                    COL_CONTENT_TEXT, "Nghe và chọn câu đúng với âm thanh.",
                    COL_SKILL_TYPE, "Nghe hiểu",
                    COL_REGION, "CENTRAL",
                    COL_AUDIO_URL, "https://example.com/audio/listening_001.mp3",
                    COL_OPTIONS, "Lúa nếp là lúa nếp làng, Lúa nết là lúa nết làng, Núa nếp là núa nếp làng",
                    COL_CORRECT_ANSWER, "Lúa nếp là lúa nếp làng",
                    COL_TRANSCRIPT, "Lúa lếp là lúa lếp làng",
                    COL_CORRECT_SENTENCE, "Lúa nếp là lúa nếp làng"));

            Row r3 = sheet.createRow(3);
            setRow(r3, headers, Map.of(
                    COL_CONTENT_TEXT, "Sắp xếp lại câu đúng.",
                    COL_SKILL_TYPE, "Viết",
                    COL_REGION, "SOUTH",
                    COL_HINT, "Sắp xếp theo ngữ nghĩa.",
                    COL_SCRAMBLED_WORDS, "Lúa, nếp, là, lúa, nếp, làng",
                    COL_CORRECT_SENTENCE, "Lúa nếp là lúa nếp làng"));

            autosize(sheet, headers.size());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo template Excel kho câu hỏi (Admin)", e);
        }
    }

    /**
     * Thêm Data Validation dạng dropdown (explicit list) cho 1 cột theo tên header.
     * - Áp dụng cho phạm vi row 2..1000 để người dùng nhập liệu thoải mái.
     * - Bật showErrorBox để hiển thị cảnh báo khi nhập sai giá trị Enum.
     */
    private void addDropdownValidation(Sheet sheet, List<String> headers, String headerKey, String[] allowedValues) {
        // Tìm index cột theo headerKey (vd: "skillType", "region")
        int colIdx = headers.indexOf(headerKey);
        if (colIdx < 0)
            return; // Không có cột thì bỏ qua an toàn

        // Chỉ hỗ trợ XSSF (xlsx) theo đúng yêu cầu dùng XSSFDataValidationHelper
        if (!(sheet instanceof XSSFSheet xssfSheet))
            return;

        // Dòng 2..1000 => 1..999 (0-based)
        CellRangeAddressList addressList = new CellRangeAddressList(1, 999, colIdx, colIdx);

        // Tạo dropdown list constraint theo danh sách allowedValues
        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(xssfSheet);
        XSSFDataValidationConstraint constraint = (XSSFDataValidationConstraint) helper
                .createExplicitListConstraint(allowedValues);

        XSSFDataValidation validation = (XSSFDataValidation) helper.createValidation(constraint, addressList);

        // Hiển thị mũi tên dropdown + bật error box khi người dùng nhập sai
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);

        xssfSheet.addValidationData(validation);
    }

    @Transactional
    public ImportResult importFromExcel(MultipartFile file) {
        int success = 0;
        int skip = 0;
        int error = 0;
        List<String> messages = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null)
                throw new RuntimeException("Không tìm thấy sheet dữ liệu");

            Row header = sheet.getRow(0);
            if (header == null)
                throw new RuntimeException("File Excel không có header");

            Map<String, Integer> colMap = buildColMap(header);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null)
                    continue;

                String contentText = get(row, colMap, COL_CONTENT_TEXT).trim();
                String skillTypeRaw = get(row, colMap, COL_SKILL_TYPE).trim();
                if (contentText.isBlank() && skillTypeRaw.isBlank())
                    continue;

                if (contentText.isBlank() || skillTypeRaw.isBlank()) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Thiếu contentText hoặc skillType");
                    continue;
                }

                SkillType skillType;
                try {
                    // Map tiếng Việt -> Enum nội bộ
                    // Nếu ô trống hoặc không khớp mapping, sẽ throw để ghi nhận lỗi theo dòng.
                    skillType = mapSkillTypeFromVietnamese(skillTypeRaw);
                } catch (Exception e) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Cột Kỹ năng có giá trị không hợp lệ: " + skillTypeRaw);
                    continue;
                }

                // Map string code nội bộ (NORTH/CENTRAL/SOUTH)
                String region;
                try {
                    region = mapRegionFromExcel(get(row, colMap, COL_REGION));
                } catch (Exception e) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Cột Vùng miền có giá trị không hợp lệ");
                    continue;
                }
                String hint = get(row, colMap, COL_HINT);

                // Idempotent skip: contentText + skillType
                boolean exists = challengeBankRepository.existsByContentTextAndSkillType(contentText, skillType);
                if (exists) {
                    skip++;
                    messages.add("Dòng " + (r + 1) + ": Bỏ qua — đã tồn tại (contentText + skillType)");
                    continue;
                }

                try {
                    Map<String, Object> metadata = buildMetadata(skillType, row, colMap, hint);

                    ChallengeBankRequest req = ChallengeBankRequest.builder()
                            .contentText(contentText)
                            .skillType(skillType)
                            .region(region)
                            .metadataJson(metadata)
                            .build();

                    challengeBankService.createChallenge(req);
                    success++;
                } catch (Exception e) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Lỗi — "
                            + (e.getMessage() != null ? e.getMessage() : "Không xác định"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file Excel kho câu hỏi (Admin): " + e.getMessage(), e);
        }

        messages.add(0, String.format("Import hoàn tất: %d thành công, %d bỏ qua, %d lỗi", success, skip, error));
        return new ImportResult(success, skip, error, messages);
    }

    @Transactional(readOnly = true)
    public byte[] exportToExcel(SkillType skillType) {
        List<ChallengeBank> data = (skillType != null)
                ? challengeBankRepository.findBySkillType(skillType)
                : challengeBankRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("ChallengeBank");
            List<String> headers = headers();

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < data.size(); i++) {
                ChallengeBank cb = data.get(i);
                Row row = sheet.createRow(i + 1);
                Map<String, Object> meta = cb.getMetadataJson() != null ? cb.getMetadataJson() : Map.of();

                put(row, headers, COL_CONTENT_TEXT, cb.getContentText());
                // Export: map enum/code nội bộ -> English
                put(row, headers, COL_SKILL_TYPE,
                        cb.getSkillType() != null ? mapSkillTypeToVietnamese(cb.getSkillType()) : "");
                put(row, headers, COL_REGION, cb.getRegion() != null ? mapRegionToExcel(cb.getRegion()) : "NORTH");

                // hint
                put(row, headers, COL_HINT, str(meta.get("hint")));

                // READING fields
                put(row, headers, COL_SENTENCE_WITH_ERROR, str(meta.get("sentenceWithError")));
                put(row, headers, COL_WRONG_WORD, str(meta.get("wrongWord")));
                put(row, headers, COL_CORRECT_WORD, str(meta.get("correctWord")));
                put(row, headers, COL_AUDIO_URL, str(meta.get("audioUrl")));
                put(row, headers, COL_OPTIONS, joinComma(meta.get("options")));
                put(row, headers, COL_CORRECT_ANSWER, str(meta.get("correctAnswer")));
                put(row, headers, COL_TRANSCRIPT, str(meta.get("transcript")));
                put(row, headers, COL_SCRAMBLED_WORDS, joinComma(meta.get("scrambledWords")));
                put(row, headers, COL_CORRECT_SENTENCE, str(meta.get("correctSentence")));
            }

            autosize(sheet, headers.size());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể export Excel kho câu hỏi (Admin)", e);
        }
    }

    private List<String> headers() {
        return List.of(
                COL_CONTENT_TEXT, COL_SKILL_TYPE, COL_REGION, COL_HINT,
                // READING
                COL_SENTENCE_WITH_ERROR, COL_WRONG_WORD, COL_CORRECT_WORD,
                // LISTENING
                COL_AUDIO_URL, COL_OPTIONS, COL_CORRECT_ANSWER, COL_TRANSCRIPT,
                // WRITING
                COL_SCRAMBLED_WORDS, COL_CORRECT_SENTENCE);
    }

    private Map<String, Object> buildMetadata(SkillType skillType, Row row, Map<String, Integer> colMap, String hint) {
        Map<String, Object> meta = new LinkedHashMap<>();

        // hint là cột cố định → nhét vào metadataJson để đồng bộ UI hiện tại
        if (hint != null && !hint.isBlank()) {
            meta.put("hint", hint);
        } else {
            meta.put("hint", "");
        }

        switch (skillType) {
            case READING -> {
                meta.put("sentenceWithError", get(row, colMap, COL_SENTENCE_WITH_ERROR));
                meta.put("wrongWord", get(row, colMap, COL_WRONG_WORD));
                meta.put("correctWord", get(row, colMap, COL_CORRECT_WORD));
            }
            case LISTENING -> {
                meta.put("audioUrl", get(row, colMap, COL_AUDIO_URL));
                meta.put("options", splitComma(get(row, colMap, COL_OPTIONS)));
                meta.put("correctAnswer", get(row, colMap, COL_CORRECT_ANSWER));
                meta.put("transcript", get(row, colMap, COL_TRANSCRIPT));
                meta.put("correctSentence", get(row, colMap, COL_CORRECT_SENTENCE));
            }
            case WRITING -> {
                meta.put("scrambledWords", splitComma(get(row, colMap, COL_SCRAMBLED_WORDS)));
                meta.put("correctSentence", get(row, colMap, COL_CORRECT_SENTENCE));
            }
            case SPEAKING, ENTRY_TEST -> {
                meta.put("audioUrl", get(row, colMap, COL_AUDIO_URL));
                meta.put("transcript", get(row, colMap, COL_TRANSCRIPT));
                meta.put("correctSentence", get(row, colMap, COL_CORRECT_SENTENCE));
            }
        }
        return meta;
    }

    private String parseRegion(String s) {
        if (s == null || s.isBlank())
            return "BAC";
        String upper = s.trim().toUpperCase();
        return switch (upper) {
            case "BAC", "BẮC", "NORTH" -> "BAC";
            case "TRUNG", "CENTRAL" -> "TRUNG";
            case "NAM", "SOUTH" -> "NAM";
            default -> "BAC";
        };
    }

    // =========================================================
    // MAPPER: Excel (Tiếng Việt) <-> System (Enum/Code)
    // =========================================================
    // Yêu cầu:
    // - Excel hiển thị tiếng Việt để người dùng chọn.
    // - Import phải map tiếng Việt -> giá trị hệ thống đang dùng.
    // - Nếu ô trống hoặc không khớp mapping: ném lỗi để báo "Dòng X: cột ... không
    // hợp lệ".

    /**
     * Map cột Kỹ năng từ tiếng Việt -> Enum SkillType.
     * Ví dụ: "Đọc hiểu" -> SkillType.READING
     */
    private SkillType mapSkillTypeFromVietnamese(String input) {
        String s = normalizeExcelText(input);
        if (s.isBlank()) {
            throw new IllegalArgumentException("Kỹ năng bị trống");
        }
        return switch (s) {
            case "đọc hiểu" -> SkillType.READING;
            case "nghe hiểu" -> SkillType.LISTENING;
            case "viết" -> SkillType.WRITING;
            case "nói" -> SkillType.SPEAKING;
            case "kiểm tra đầu vào" -> SkillType.ENTRY_TEST;
            default -> throw new IllegalArgumentException("Kỹ năng không hợp lệ: " + input);
        };
    }

    /**
     * Map Enum SkillType -> tiếng Việt để ghi ra Excel.
     */
    private String mapSkillTypeToVietnamese(SkillType st) {
        if (st == null)
            return "";
        return switch (st) {
            case READING -> "Đọc hiểu";
            case LISTENING -> "Nghe hiểu";
            case WRITING -> "Viết";
            case SPEAKING -> "Nói";
            case ENTRY_TEST -> "Kiểm tra đầu vào";
        };
    }

    /**
     * Map cột Vùng miền từ Excel -> String code hệ thống dùng trong
     * ChallengeBankRequest.
     * Ví dụ: "NORTH" -> "BAC" (Internal DB still uses BAC/TRUNG/NAM for now or we
     * can migrate)
     * Let's keep internal as it is but allow English in Excel.
     */
    private String mapRegionFromExcel(String input) {
        String s = normalizeExcelText(input);
        if (s.isBlank()) {
            throw new IllegalArgumentException("Region is empty");
        }
        return switch (s) {
            case "north", "miền bắc", "bac" -> "BAC";
            case "central", "miền trung", "trung" -> "TRUNG";
            case "south", "miền nam", "nam" -> "NAM";
            default -> throw new IllegalArgumentException("Invalid region: " + input);
        };
    }

    /**
     * Map String code nội bộ (BAC/TRUNG/NAM) -> English để ghi ra Excel.
     */
    private String mapRegionToExcel(String regionCode) {
        if (regionCode == null || regionCode.isBlank())
            return "NORTH";
        return switch (regionCode.toUpperCase()) {
            case "BAC" -> "NORTH";
            case "TRUNG" -> "CENTRAL";
            case "NAM" -> "SOUTH";
            default -> "NORTH";
        };
    }

    /**
     * Normalize text lấy từ Excel:
     * - Trim khoảng trắng
     * - Hạ chữ thường để switch-case ổn định
     */
    private String normalizeExcelText(String input) {
        return input == null ? "" : input.trim().toLowerCase();
    }

    private Map<String, Integer> buildColMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null)
                continue;
            String name = cellString(cell).trim();
            if (!name.isBlank())
                map.put(name, i);
        }
        // Validate required fixed columns
        for (String required : List.of(COL_CONTENT_TEXT, COL_SKILL_TYPE, COL_REGION)) {
            if (!map.containsKey(required))
                throw new RuntimeException("Thiếu cột bắt buộc: " + required);
        }
        return map;
    }

    private String get(Row row, Map<String, Integer> colMap, String colName) {
        Integer idx = colMap.get(colName);
        if (idx == null)
            return "";
        return cellString(row.getCell(idx));
    }

    private void setRow(Row row, List<String> headers, Map<String, String> values) {
        for (int i = 0; i < headers.size(); i++) {
            String key = headers.get(i);
            String val = values.getOrDefault(key, "");
            row.createCell(i).setCellValue(val);
        }
    }

    private void put(Row row, List<String> headers, String key, String value) {
        int idx = headers.indexOf(key);
        if (idx < 0)
            return;
        row.createCell(idx).setCellValue(value != null ? value : "");
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 18 * 256)
                sheet.setColumnWidth(i, 18 * 256);
        }
    }

    private String cellString(Cell cell) {
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                long lv = (long) v;
                yield (Math.abs(v - lv) < 0.0000001) ? String.valueOf(lv) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield "";
                }
            }
            default -> "";
        };
    }

    private List<String> splitComma(String raw) {
        if (raw == null)
            return List.of();
        String s = raw.trim();
        if (s.isBlank())
            return List.of();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .toList();
    }

    private int parseInt(String raw, int fallback) {
        if (raw == null)
            return fallback;
        String s = raw.trim();
        if (s.isBlank())
            return fallback;
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String joinComma(Object value) {
        if (value instanceof List<?> list) {
            return String.join(", ", list.stream().map(Object::toString).toList());
        }
        return value != null ? value.toString() : "";
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
    }
}
