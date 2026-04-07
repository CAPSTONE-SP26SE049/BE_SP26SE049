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
import org.fsa_2026.company_fsa_captone_2026.dto.LevelCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLevelExcelService {

    // =========================================================
    // Header cột Excel (Tiếng Việt) — thân thiện cho người nhập liệu
    // =========================================================
    // Lưu ý: dù hệ thống lưu DB theo UUID/metadata_json, file Excel sẽ dùng tiếng Việt
    // và backend sẽ tự map "Phương ngữ" <-> UUID dialect để người dùng không phải copy UUID.
    private static final String COL_NAME_VN = "Tên chương học";
    private static final String COL_DIALECT_VN = "Phương ngữ";
    private static final String COL_DESCRIPTION_VN = "Mô tả";
    private static final String COL_LEVEL_ORDER_VN = "Thứ tự level";
    private static final String COL_AI_THRESHOLD_VN = "Ngưỡng AI";
    private static final String COL_MIN_STARS_VN = "Số sao tối thiểu";
    private static final String COL_COMMENT_VN = "Ghi chú";

    // Dropdown list cố định cho "Phương ngữ" theo yêu cầu (có thể nâng cấp thành query DB động sau)
    private static final String DIALECT_NORTH_VN = "Miền Bắc";
    private static final String DIALECT_CENTRAL_VN = "Miền Trung";
    private static final String DIALECT_SOUTH_VN = "Miền Nam";

    // Type trong DB cho Dialect/Level (đang dùng trong AdminService)
    private static final String TYPE_LEVEL = "LEVEL";
    private static final String TYPE_DIALECT = "DIALECT";

    private final AdminService adminService;
    private final LearningUnitRepository learningUnitRepository;

    public record ImportResult(int successCount, int skipCount, int errorCount, List<String> messages) {}

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Levels");
            List<String> headers = List.of(
                    COL_NAME_VN,
                    COL_DIALECT_VN,
                    COL_DESCRIPTION_VN,
                    COL_LEVEL_ORDER_VN,
                    COL_AI_THRESHOLD_VN,
                    COL_MIN_STARS_VN,
                    COL_COMMENT_VN
            );

            // Tạo header + style như hiện tại (không thay đổi logic cũ)
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // =========================
            // Data Validation (Dropdown list) cho cột "Phương ngữ"
            // - Người dùng chọn tiếng Việt (Miền Bắc/Trung/Nam)
            // - Khi import, backend sẽ map tiếng Việt -> tên dialect trong DB -> UUID parentId
            // Áp dụng phạm vi row 2..1000 để nhập liệu thoải mái.
            // =========================
            addDropdownValidationIfPresent(sheet, headers, COL_DIALECT_VN,
                    new String[]{DIALECT_NORTH_VN, DIALECT_CENTRAL_VN, DIALECT_SOUTH_VN});

            // Sample rows (Phương ngữ là tiếng Việt, KHÔNG yêu cầu UUID)
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("Nhóm âm L/N - Cơ bản");
            r1.createCell(1).setCellValue(DIALECT_SOUTH_VN);
            r1.createCell(2).setCellValue("Luyện phân biệt L/N qua từ vựng cơ bản.");
            r1.createCell(3).setCellValue(1);
            r1.createCell(4).setCellValue(75);
            r1.createCell(5).setCellValue(3);
            r1.createCell(6).setCellValue("Có thể để trống các cột tuỳ chọn");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("Nhóm âm CH/TR - Trung bình");
            r2.createCell(1).setCellValue(DIALECT_CENTRAL_VN);
            r2.createCell(2).setCellValue("Bài tập nghe-nói để phân biệt CH/TR.");
            r2.createCell(3).setCellValue(2);
            r2.createCell(4).setCellValue(80);
            r2.createCell(5).setCellValue(4);
            r2.createCell(6).setCellValue("");

            autosize(sheet, headers.size());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo template Excel chương học", e);
        }
    }

    /**
     * Thêm Data Validation dạng dropdown (explicit list) nếu template có cột tương ứng.
     * - Dùng XSSFDataValidationHelper/Constraint theo yêu cầu.
     * - Bật showErrorBox để hiển thị cảnh báo nhập sai Enum.
     */
    private void addDropdownValidationIfPresent(Sheet sheet, List<String> headers, String headerKey, String[] allowedValues) {
        int colIdx = headers.indexOf(headerKey);
        if (colIdx < 0) return; // Không có cột thì không làm gì
        if (!(sheet instanceof XSSFSheet xssfSheet)) return; // Chỉ áp dụng cho .xlsx

        // Row 2..1000 => index 1..999
        CellRangeAddressList addressList = new CellRangeAddressList(1, 999, colIdx, colIdx);

        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(xssfSheet);
        XSSFDataValidationConstraint constraint = (XSSFDataValidationConstraint)
                helper.createExplicitListConstraint(allowedValues);
        XSSFDataValidation validation = (XSSFDataValidation) helper.createValidation(constraint, addressList);

        // Hiển thị dropdown + bật error box
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);

        xssfSheet.addValidationData(validation);
    }

    public ImportResult importFromExcel(MultipartFile file) {
        int success = 0;
        int skip = 0;
        int error = 0;
        List<String> messages = new ArrayList<>();

        // Quan trọng: KHÔNG dùng @Transactional cho cả hàm import.
        // Lý do: chỉ cần 1 dòng gặp lỗi DB (duplicate/constraint) là toàn bộ transaction bị rollback-only,
        // dẫn tới lỗi "Transaction silently rolled back..." và làm mất các dòng đã import thành công.
        // Giải pháp: check-before-insert bằng Repository để hạn chế tối đa lỗi DB.

        // Dùng để tránh tạo trùng theo (dialectId + name) trong chính file import (tối ưu giảm query)
        Set<String> existingKeys = new HashSet<>();
        try {
            for (LevelResponse lr : adminService.getAllLevels()) {
                String key = (lr.getDialectId() != null ? lr.getDialectId() : "") + "|" + (lr.getName() != null ? lr.getName().toLowerCase() : "");
                existingKeys.add(key);
            }
        } catch (Exception ignored) {
            // nếu lỗi export danh sách level thì vẫn cho import, backend sẽ tự xử lý trùng nếu có
        }

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) throw new RuntimeException("Không tìm thấy sheet dữ liệu");

            Row header = sheet.getRow(0);
            if (header == null) throw new RuntimeException("File Excel không có header");

            // Đồng bộ header tiếng Việt: tìm index theo tên cột tiếng Việt
            int nameCol = findColumnIndex(header, COL_NAME_VN);
            int dialectCol = findColumnIndex(header, COL_DIALECT_VN);
            int descCol = findColumnIndex(header, COL_DESCRIPTION_VN);
            int orderCol = findOptionalColumnIndex(header, COL_LEVEL_ORDER_VN);
            int aiCol = findOptionalColumnIndex(header, COL_AI_THRESHOLD_VN);
            int minStarsCol = findOptionalColumnIndex(header, COL_MIN_STARS_VN);
            int commentCol = findOptionalColumnIndex(header, COL_COMMENT_VN);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String name = cellString(row.getCell(nameCol)).trim();
                String dialectText = cellString(row.getCell(dialectCol)).trim();
                String description = cellString(row.getCell(descCol)).trim();

                if (name.isBlank() && dialectText.isBlank() && description.isBlank()) continue;

                // UI chỉ bắt buộc: Tên + Phương ngữ + Mô tả
                if (name.isBlank() || dialectText.isBlank() || description.isBlank()) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Thiếu Tên chương học hoặc Phương ngữ hoặc Mô tả");
                    continue;
                }

                // Parse các cột tuỳ chọn (có thể trống → set default)
                Integer levelOrder = orderCol >= 0 ? parseInt(cellString(row.getCell(orderCol)), 1) : 1;
                Integer aiThreshold = aiCol >= 0 ? parseInt(cellString(row.getCell(aiCol)), 75) : 75;
                Integer minStars = minStarsCol >= 0 ? parseInt(cellString(row.getCell(minStarsCol)), 3) : 3;
                String comment = commentCol >= 0 ? cellString(row.getCell(commentCol)).trim() : "";

                try {
                    // =========================
                    // Mapping "Phương ngữ" (Tiếng Việt) -> UUID dialect trong DB
                    // =========================
                    // 1) Người dùng chọn: Miền Bắc/Miền Trung/Miền Nam (tiếng Việt).
                    // 2) Backend map sang "mã dialect" đang lưu trong DB (ví dụ: NORTH/CENTRAL/SOUTH).
                    // 3) Query LearningUnitRepository để lấy dialect entity và UUID thật.
                    // Nếu không tìm thấy dialect tương ứng → báo lỗi "Phương ngữ không hợp lệ".
                    String dialectKey = mapDialectVietnameseToDbName(dialectText);
                    UUID parentId = learningUnitRepository.findByTypeAndNameIgnoreCase(TYPE_DIALECT, dialectKey)
                            .orElseThrow(() -> new RuntimeException("Phương ngữ không hợp lệ: " + dialectText))
                            .getId();

                    // 1) Check-before-insert tại DB: tránh lỗi duplicate key/constraint
                    // Quy tắc: level được xem là trùng nếu (type=LEVEL + name) trùng (ignore case).
                    // Lưu ý: nếu business muốn trùng theo (parentId+name) thì cần thêm method repo tương ứng.
                    if (learningUnitRepository.findByTypeAndNameIgnoreCase(TYPE_LEVEL, name).isPresent()) {
                        skip++;
                        messages.add("Dòng " + (r + 1) + ": Bỏ qua - Tên chương đã tồn tại (" + name + ")");
                        continue;
                    }

                    // Check trùng ngay trong batch theo (dialectId + name) để không gọi create dư thừa
                    String key = parentId.toString() + "|" + name.toLowerCase();
                    if (existingKeys.contains(key)) {
                        skip++;
                        messages.add("Dòng " + (r + 1) + ": Bỏ qua — chương đã tồn tại trong file import (" + name + ")");
                        continue;
                    }

                    Map<String, Object> metadataJson = new LinkedHashMap<>();
                    // logic hiện tại: adminService.createLevel() luôn ép status = APPROVED
                    metadataJson.put("status", "APPROVED");
                    metadataJson.put("audio_url", null);
                    metadataJson.put("level_order", levelOrder != null ? levelOrder : 1);
                    metadataJson.put("ai_threshold", aiThreshold);
                    metadataJson.put("error_tag_id", null);
                    metadataJson.put("rejection_reason", null);
                    metadataJson.put("min_stars_required", minStars != null ? minStars : 3);
                    metadataJson.put("description", description);

                    LevelCreateRequest req = LevelCreateRequest.builder()
                            .name(name)
                            .type(TYPE_LEVEL)
                            .parentId(parentId)
                            .metadataJson(metadataJson)
                            .comment(comment != null && !comment.isBlank() ? comment : null)
                            .build();

                    adminService.createLevel(req);
                    existingKeys.add(key);
                    success++;
                } catch (IllegalArgumentException e) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Dữ liệu không hợp lệ");
                } catch (Exception e) {
                    // 2) Vẫn bọc try-catch để bắt các lỗi rủi ro hệ thống khác.
                    // Mục tiêu chính: không để lỗi duplicate/constraint quăng ra bằng check-before-insert phía trên.
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Lỗi — " + (e.getMessage() != null ? e.getMessage() : "Không xác định"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file Excel chương học: " + e.getMessage(), e);
        }

        messages.add(0, String.format("Import hoàn tất: %d thành công, %d bỏ qua, %d lỗi", success, skip, error));
        return new ImportResult(success, skip, error, messages);
    }

    @Transactional(readOnly = true)
    public byte[] exportToExcel() {
        // Export TOÀN BỘ danh sách Level hiện có trong DB
        List<LevelResponse> levels = adminService.getAllLevels();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Levels");
            List<String> headers = List.of(
                    COL_NAME_VN,
                    COL_DIALECT_VN,
                    COL_DESCRIPTION_VN,
                    COL_LEVEL_ORDER_VN,
                    COL_AI_THRESHOLD_VN,
                    COL_MIN_STARS_VN,
                    COL_COMMENT_VN
            );

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < levels.size(); i++) {
                LevelResponse l = levels.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(l.getName() != null ? l.getName() : "");

                // =========================
                // Mapping UUID dialectId -> Tên Phương ngữ tiếng Việt khi export
                // =========================
                // 1) LevelResponse chỉ có dialectId (UUID dạng string).
                // 2) Query LearningUnitRepository để lấy dialect entity (type=DIALECT).
                // 3) Dựa vào dialect.name trong DB (NORTH/CENTRAL/SOUTH) -> map ra "Miền Bắc/Trung/Nam".
                String dialectLabel = "";
                if (l.getDialectId() != null && !l.getDialectId().isBlank()) {
                    try {
                        UUID dialectId = UUID.fromString(l.getDialectId());
                        dialectLabel = learningUnitRepository.findById(dialectId)
                                .map(d -> mapDialectDbNameToVietnamese(d.getName()))
                                .orElse("");
                    } catch (Exception ignored) {
                        dialectLabel = "";
                    }
                }
                row.createCell(1).setCellValue(dialectLabel);

                row.createCell(2).setCellValue(l.getDescription() != null ? l.getDescription() : "");
                if (l.getLevelOrder() != null) row.createCell(3).setCellValue(l.getLevelOrder());
                if (l.getAiThreshold() != null) row.createCell(4).setCellValue(l.getAiThreshold());
                if (l.getMinStarsRequired() != null) row.createCell(5).setCellValue(l.getMinStarsRequired());
                row.createCell(6).setCellValue(""); // comment: không lưu trong entity hiện tại
            }

            autosize(sheet, headers.size());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể export Excel chương học", e);
        }
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
            if (sheet.getColumnWidth(i) < 18 * 256) sheet.setColumnWidth(i, 18 * 256);
        }
    }

    private int findColumnIndex(Row headerRow, String headerName) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            String s = cellString(cell);
            if (s.equalsIgnoreCase(headerName)) return i;
        }
        throw new RuntimeException("Thiếu cột bắt buộc: " + headerName);
    }

    /**
     * Tìm cột nếu có (optional). Nếu không có thì trả -1 để code import tự dùng default.
     * Dùng cho các cột tuỳ chọn như "Ngưỡng AI", "Số sao tối thiểu", ...
     */
    private int findOptionalColumnIndex(Row headerRow, String headerName) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            String s = cellString(cell);
            if (s.equalsIgnoreCase(headerName)) return i;
        }
        return -1;
    }

    private String cellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                long lv = (long) v;
                yield (Math.abs(v - lv) < 0.0000001) ? String.valueOf(lv) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); } catch (Exception e) { yield ""; }
            }
            default -> "";
        };
    }

    private Integer parseInt(String raw, Integer fallback) {
        if (raw == null) return fallback;
        String s = raw.trim();
        if (s.isBlank()) return fallback;
        try {
            // Excel numeric may come as "75.0"
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // =========================================================
    // Mapper Phương ngữ (Tiếng Việt) <-> Dialect name trong DB
    // =========================================================
    // Vì DB đang lưu dialect theo "name" dạng code (NORTH/CENTRAL/SOUTH),
    // nên import/export sẽ map qua lại để Excel thân thiện.

    /**
     * Map tiếng Việt trong Excel -> dialect.name trong DB.
     * Ví dụ: "Miền Bắc" -> "NORTH"
     */
    private String mapDialectVietnameseToDbName(String dialectText) {
        String s = dialectText == null ? "" : dialectText.trim().toLowerCase();
        return switch (s) {
            case "miền bắc" -> "NORTH";
            case "miền trung" -> "CENTRAL";
            case "miền nam" -> "SOUTH";
            default -> throw new IllegalArgumentException("Phương ngữ không hợp lệ: " + dialectText);
        };
    }

    /**
     * Map dialect.name trong DB -> tiếng Việt để hiển thị trên Excel.
     */
    private String mapDialectDbNameToVietnamese(String dbName) {
        if (dbName == null) return "";
        return switch (dbName.trim().toUpperCase()) {
            case "NORTH" -> "Miền Bắc";
            case "CENTRAL" -> "Miền Trung";
            case "SOUTH" -> "Miền Nam";
            default -> "";
        };
    }
}

