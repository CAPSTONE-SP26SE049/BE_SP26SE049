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
import org.fsa_2026.company_fsa_captone_2026.common.AdminExcelUploadValidator;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.exception.BadRequestException;
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
    private static final String COL_NAME_VN = "Tên chương / Level Name";
    private static final String COL_DIALECT_VN = "Vùng miền / Dialect (NORTH/CENTRAL/SOUTH)";
    private static final String COL_DESCRIPTION_VN = "Mô tả / Description";

    // Dropdown list cố định cho "Phương ngữ" theo yêu cầu (có thể nâng cấp thành query DB động sau)
    // Dropdown list constants
    private static final String DIALECT_NORTH = "NORTH";
    private static final String DIALECT_CENTRAL = "CENTRAL";
    private static final String DIALECT_SOUTH = "SOUTH";

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
                    COL_DESCRIPTION_VN
            );

            // Tạo header + style
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data Validation (Dropdown list) cho cột "Dialect"
            addDropdownValidationIfPresent(sheet, headers, COL_DIALECT_VN,
                    new String[]{DIALECT_NORTH, DIALECT_CENTRAL, DIALECT_SOUTH});

            // Sample rows
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("L/N Sound Group - Basic");
            r1.createCell(1).setCellValue(DIALECT_NORTH);
            r1.createCell(2).setCellValue("Practice distinguishing L/N via basic vocabulary.");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("CH/TR Sound Group - Intermediate");
            r2.createCell(1).setCellValue(DIALECT_CENTRAL);
            r2.createCell(2).setCellValue("Listening and speaking exercises for CH/TR.");

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

    /**
     * Import level từ Excel — Fix A-03/A-04/A-05/A-06: validate file + header trước POI.
     */
    public ImportResult importFromExcel(MultipartFile file) {
        // Fix A-03 & A-04 & A-06: Validate file rỗng và sai định dạng .xlsx để tránh sập server 500
        AdminExcelUploadValidator.validateXlsxUpload(file);

        int success = 0;
        int skip = 0;
        int error = 0;
        List<String> messages = new ArrayList<>();

        // Dùng để tránh tạo trùng theo (dialectId + name) trong chính file import
        Set<String> existingKeys = new HashSet<>();
        try {
            for (LevelResponse lr : adminService.getAllLevels()) {
                String key = (lr.getDialectId() != null ? lr.getDialectId() : "") + "|" + (lr.getName() != null ? lr.getName().toLowerCase() : "");
                existingKeys.add(key);
            }
        } catch (Exception ignored) {
        }

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BadRequestException("Không tìm thấy sheet dữ liệu trong file Excel");
            }

            Row header = sheet.getRow(0);
            if (header == null) {
                throw new BadRequestException("File Excel không có header");
            }

            // Chỉ 3 cột bắt buộc — khớp với form "Tạo chương học mới"
            int nameCol = findColumnIndex(header, COL_NAME_VN);
            int dialectCol = findColumnIndex(header, COL_DIALECT_VN);
            int descCol = findColumnIndex(header, COL_DESCRIPTION_VN);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String name = cellString(row.getCell(nameCol)).trim();
                String dialectText = cellString(row.getCell(dialectCol)).trim();
                String description = cellString(row.getCell(descCol)).trim();

                if (name.isBlank() && dialectText.isBlank() && description.isBlank()) continue;

                // Bắt buộc: Tên + Phương ngữ (Mô tả có thể trống)
                if (name.isBlank() || dialectText.isBlank()) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Thiếu Tên chương học hoặc Phương ngữ");
                    continue;
                }

                try {
                    // Mapping "Dialect" (English) -> UUID dialect trong DB
                    String dialectKey = mapDialectExcelToDbName(dialectText);
                    UUID parentId = learningUnitRepository.findByTypeAndNameIgnoreCase(TYPE_DIALECT, dialectKey)
                            .orElseThrow(() -> new RuntimeException("Invalid dialect: " + dialectText))
                            .getId();

                    // Check trùng tại DB
                    if (learningUnitRepository.findByTypeAndNameIgnoreCase(TYPE_LEVEL, name).isPresent()) {
                        skip++;
                        messages.add("Dòng " + (r + 1) + ": Bỏ qua - Tên chương đã tồn tại (" + name + ")");
                        continue;
                    }

                    // Check trùng trong batch
                    String key = parentId.toString() + "|" + name.toLowerCase();
                    if (existingKeys.contains(key)) {
                        skip++;
                        messages.add("Dòng " + (r + 1) + ": Bỏ qua — chương đã tồn tại trong file import (" + name + ")");
                        continue;
                    }

                    // Dùng default values cho các trường ẩn (giống form tạo mới)
                    Map<String, Object> metadataJson = new LinkedHashMap<>();
                    metadataJson.put("status", "APPROVED");
                    metadataJson.put("audio_url", null);
                    metadataJson.put("level_order", 1);
                    metadataJson.put("ai_threshold", 75);
                    metadataJson.put("error_tag_id", null);
                    metadataJson.put("rejection_reason", null);
                    metadataJson.put("min_stars_required", 3);
                    metadataJson.put("description", description);

                    LevelCreateRequest req = LevelCreateRequest.builder()
                            .name(name)
                            .type(TYPE_LEVEL)
                            .parentId(parentId)
                            .metadataJson(metadataJson)
                            .build();

                    adminService.createLevel(req);
                    existingKeys.add(key);
                    success++;
                } catch (IllegalArgumentException e) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Dữ liệu không hợp lệ");
                } catch (Exception e) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Lỗi — " + (e.getMessage() != null ? e.getMessage() : "Không xác định"));
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            // Lỗi parse POI sau khi đã validate — vẫn trả 400 thay vì RuntimeException/500
            String detail = e.getMessage() != null ? e.getMessage() : "Định dạng file không hợp lệ";
            throw new BadRequestException("Không thể đọc file Excel chương học: " + detail);
        }

        messages.add(0, String.format("Import hoàn tất: %d thành công, %d bỏ qua, %d lỗi", success, skip, error));
        return new ImportResult(success, skip, error, messages);
    }

    @Transactional(readOnly = true)
    public byte[] exportToExcel() {
        List<LevelResponse> levels = adminService.getAllLevels();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Levels");
            List<String> headers = List.of(
                    COL_NAME_VN,
                    COL_DIALECT_VN,
                    COL_DESCRIPTION_VN
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

                // Mapping UUID dialectId -> English dialect name khi export
                String dialectLabel = "";
                if (l.getDialectId() != null && !l.getDialectId().isBlank()) {
                    try {
                        UUID dialectId = UUID.fromString(l.getDialectId());
                        dialectLabel = learningUnitRepository.findById(dialectId)
                                .map(d -> mapDialectDbNameToExcel(d.getName()))
                                .orElse("");
                    } catch (Exception ignored) {
                        dialectLabel = "";
                    }
                }
                row.createCell(1).setCellValue(dialectLabel);

                row.createCell(2).setCellValue(l.getDescription() != null ? l.getDescription() : "");
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

    // Fix A-05: thiếu cột bắt buộc → 400 BadRequest, không RuntimeException/500
    private int findColumnIndex(Row headerRow, String headerName) {
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            String s = cellString(cell);
            if (s.equalsIgnoreCase(headerName)) return i;
        }
        throw new BadRequestException("Thiếu cột bắt buộc: " + headerName);
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

    // =========================================================
    // Mapper Dialect (English) <-> Dialect name trong DB
    // =========================================================

    /**
     * Map English in Excel -> dialect.name trong DB.
     * Ví dụ: "NORTH" -> "NORTH"
     */
    private String mapDialectExcelToDbName(String dialectText) {
        String s = dialectText == null ? "" : dialectText.trim().toLowerCase();
        return switch (s) {
            case "north", "miền bắc" -> "NORTH";
            case "central", "miền trung" -> "CENTRAL";
            case "south", "miền nam" -> "SOUTH";
            default -> throw new IllegalArgumentException("Invalid dialect: " + dialectText);
        };
    }

    /**
     * Map dialect.name trong DB -> English để hiển thị trên Excel.
     */
    private String mapDialectDbNameToExcel(String dbName) {
        if (dbName == null) return "";
        return switch (dbName.trim().toUpperCase()) {
            case "NORTH" -> "NORTH";
            case "CENTRAL" -> "CENTRAL";
            case "SOUTH" -> "SOUTH";
            default -> "";
        };
    }
}
