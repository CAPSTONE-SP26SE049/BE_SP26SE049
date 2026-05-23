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
import org.fsa_2026.company_fsa_captone_2026.dto.RewardCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.RewardResponse;
import org.fsa_2026.company_fsa_captone_2026.repository.RewardCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRewardExcelService {

    private static final String COL_NAME_VN = "Tên thành tựu";
    private static final String COL_CODE_VN = "Mã code";
    private static final String COL_ICON_URL_VN = "URL Hình ảnh";
    private static final String COL_ACTIVE_VN = "Trạng thái (Hoạt động/Ngưng hoạt động)";

    private static final String STATUS_ACTIVE_VN = "Hoạt động";
    private static final String STATUS_INACTIVE_VN = "Ngưng hoạt động";

    private final AdminService adminService;
    private final RewardCatalogRepository rewardCatalogRepository;

    public record ImportResult(int successCount, int skipCount, int errorCount, List<String> messages) {}

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Achievements");
            List<String> headers = List.of(
                    COL_NAME_VN,
                    COL_CODE_VN,
                    COL_ICON_URL_VN,
                    COL_ACTIVE_VN
            );

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            addDropdownValidation(sheet, headers, COL_ACTIVE_VN,
                    new String[]{STATUS_ACTIVE_VN, STATUS_INACTIVE_VN});

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("Học giả thông thái");
            r1.createCell(1).setCellValue("ACHIEVEMENT_GENIUS");
            r1.createCell(2).setCellValue("https://res.cloudinary.com/demo/image/upload/badge1.png");
            r1.createCell(3).setCellValue(STATUS_ACTIVE_VN);

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("Thần tốc");
            r2.createCell(1).setCellValue("ACHIEVEMENT_SPEEDRUN");
            r2.createCell(2).setCellValue("");
            r2.createCell(3).setCellValue(STATUS_ACTIVE_VN);

            autosize(sheet, headers.size());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo template Excel thành tựu", e);
        }
    }

    private void addDropdownValidation(Sheet sheet, List<String> headers, String headerKey, String[] allowedValues) {
        int colIdx = headers.indexOf(headerKey);
        if (colIdx < 0) return;
        if (!(sheet instanceof XSSFSheet xssfSheet)) return;

        CellRangeAddressList addressList = new CellRangeAddressList(1, 999, colIdx, colIdx);
        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(xssfSheet);
        XSSFDataValidationConstraint constraint = (XSSFDataValidationConstraint)
                helper.createExplicitListConstraint(allowedValues);
        XSSFDataValidation validation = (XSSFDataValidation) helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);
        xssfSheet.addValidationData(validation);
    }

    public ImportResult importFromExcel(MultipartFile file) {
        int success = 0;
        int skip = 0;
        int error = 0;
        List<String> messages = new ArrayList<>();

        Set<String> existingCodes = new HashSet<>();
        try {
            rewardCatalogRepository.findAll().forEach(r -> existingCodes.add(r.getCode().toLowerCase()));
        } catch (Exception ignored) {}

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) throw new RuntimeException("Không tìm thấy sheet dữ liệu");

            Row header = sheet.getRow(0);
            if (header == null) throw new RuntimeException("File Excel không có header");

            int nameCol = findColumnIndex(header, COL_NAME_VN);
            int codeCol = findColumnIndex(header, COL_CODE_VN);
            int iconCol = findColumnIndex(header, COL_ICON_URL_VN);
            int activeCol = findColumnIndex(header, COL_ACTIVE_VN);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String name = cellString(row.getCell(nameCol)).trim();
                String code = cellString(row.getCell(codeCol)).trim();
                String iconUrl = cellString(row.getCell(iconCol)).trim();
                String activeStr = cellString(row.getCell(activeCol)).trim();

                if (name.isBlank() && code.isBlank()) continue;

                if (name.isBlank() || code.isBlank()) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Thiếu Tên hoặc Mã code");
                    continue;
                }

                if (existingCodes.contains(code.toLowerCase())) {
                    skip++;
                    messages.add("Dòng " + (r + 1) + ": Bỏ qua - Mã code đã tồn tại (" + code + ")");
                    continue;
                }

                try {
                    RewardCreateRequest req = RewardCreateRequest.builder()
                            .name(name)
                            .code(code)
                            .iconUrl(iconUrl.isBlank() ? null : iconUrl)
                            .isActive(activeStr.equalsIgnoreCase(STATUS_INACTIVE_VN) ? false : true)
                            .build();

                    adminService.createReward(req);
                    existingCodes.add(code.toLowerCase());
                    success++;
                } catch (Exception e) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Lỗi — " + (e.getMessage() != null ? e.getMessage() : "Không xác định"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file Excel thành tựu: " + e.getMessage(), e);
        }

        messages.add(0, String.format("Import hoàn tất: %d thành công, %d bỏ qua, %d lỗi", success, skip, error));
        return new ImportResult(success, skip, error, messages);
    }

    @Transactional(readOnly = true)
    public byte[] exportToExcel() {
        List<RewardResponse> rewards = adminService.getAllRewards();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Achievements");
            List<String> headers = List.of(
                    COL_NAME_VN,
                    COL_CODE_VN,
                    COL_ICON_URL_VN,
                    COL_ACTIVE_VN
            );

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < rewards.size(); i++) {
                RewardResponse r = rewards.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(r.getName());
                row.createCell(1).setCellValue(r.getCode());
                row.createCell(2).setCellValue(r.getIconUrl() != null ? r.getIconUrl() : "");
                row.createCell(3).setCellValue(r.isActive() ? STATUS_ACTIVE_VN : STATUS_INACTIVE_VN);
            }

            autosize(sheet, headers.size());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể export Excel thành tựu", e);
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 20 * 256) sheet.setColumnWidth(i, 20 * 256);
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
            default -> "";
        };
    }
}
