package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.UserManagementResponse;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserExcelService {

    private static final String COL_EMAIL = "Email";
    private static final String COL_FULL_NAME = "FullName";

    private final AdminService adminService;
    private final AccountRepository accountRepository;

    // Regex đơn giản để check email trước khi gọi tạo tài khoản (tránh đẩy lỗi xuống DB)
    // Lưu ý: regex này là "đủ dùng" cho import, không thay thế hoàn toàn @Email của backend.
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public record ImportResult(int successCount, int skipCount, int errorCount, List<String> messages) {}

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Teachers");

            List<String> headers = List.of(COL_EMAIL, COL_FULL_NAME);

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // sample rows
            sheet.createRow(1).createCell(0).setCellValue("teacher.lan1@speakvn.edu.vn");
            sheet.getRow(1).createCell(1).setCellValue("Trần Thị Lan");

            sheet.createRow(2).createCell(0).setCellValue("teacher.minh2@speakvn.edu.vn");
            sheet.getRow(2).createCell(1).setCellValue("Nguyễn Hoàng Minh");

            autosize(sheet, headers.size());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo template Excel giáo viên", e);
        }
    }

    public ImportResult importFromExcel(MultipartFile file) {
        int success = 0;
        int skip = 0;
        int error = 0;
        List<String> messages = new ArrayList<>();

        // Quan trọng: KHÔNG dùng @Transactional cho cả hàm import.
        // Lý do: nếu một dòng gặp lỗi DB, transaction sẽ bị đánh dấu rollback-only,
        // gây ra lỗi "Transaction silently rolled back..." và làm mất cả các dòng trước đó.
        // Thay vào đó, dùng "check-before-insert" để giảm tối đa lỗi duplicate/constraint.

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) throw new RuntimeException("Không tìm thấy sheet dữ liệu");

            Row header = sheet.getRow(0);
            if (header == null) throw new RuntimeException("File Excel không có header");

            int emailCol = findColumnIndex(header, COL_EMAIL);
            int fullNameCol = findColumnIndex(header, COL_FULL_NAME);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String email = cellString(row.getCell(emailCol)).trim();
                String fullName = cellString(row.getCell(fullNameCol)).trim();

                if (email.isBlank() && fullName.isBlank()) continue;
                if (email.isBlank() || fullName.isBlank()) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Thiếu Email hoặc FullName");
                    continue;
                }

                // 1) Check định dạng email thủ công (tránh gọi create rồi mới fail validation/DB)
                if (!EMAIL_REGEX.matcher(email).matches()) {
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Lỗi định dạng Email");
                    continue;
                }

                // 2) Check-before-insert: nếu email đã tồn tại thì skip ngay, KHÔNG gọi create nữa
                if (accountRepository.existsByEmail(email)) {
                    skip++;
                    messages.add("Dòng " + (r + 1) + ": Bỏ qua - Email đã tồn tại (" + email + ")");
                    continue;
                }

                try {
                    adminService.createEducatorAccount(EducatorCreateRequest.builder()
                            .email(email)
                            .fullName(fullName)
                            .build());
                    success++;
                } catch (Exception e) {
                    // 3) Vẫn bọc try-catch để bắt các lỗi rủi ro hệ thống khác (mail, mapping, ...)
                    // Mục tiêu chính: không để lỗi duplicate/constraint xảy ra nhờ check-before-insert ở trên.
                    error++;
                    messages.add("Dòng " + (r + 1) + ": Lỗi hệ thống — " + (e.getMessage() != null ? e.getMessage() : "Không xác định"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể đọc file Excel giáo viên: " + e.getMessage(), e);
        }

        messages.add(0, String.format("Import hoàn tất: %d thành công, %d bỏ qua, %d lỗi", success, skip, error));
        return new ImportResult(success, skip, error, messages);
    }

    @Transactional(readOnly = true)
    public byte[] exportToExcel() {
        List<UserManagementResponse> users = adminService.getAllUsers();
        List<UserManagementResponse> educators = users.stream()
                .filter(u -> u.getRoleCode() != null && u.getRoleCode().equalsIgnoreCase("EDUCATOR"))
                .toList();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Teachers");
            List<String> headers = List.of(COL_EMAIL, COL_FULL_NAME);

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < educators.size(); i++) {
                UserManagementResponse u = educators.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(u.getEmail() != null ? u.getEmail() : "");
                row.createCell(1).setCellValue(u.getFullName() != null ? u.getFullName() : "");
            }

            autosize(sheet, headers.size());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể export Excel giáo viên", e);
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
            if (sheet.getColumnWidth(i) < 18 * 256) {
                sheet.setColumnWidth(i, 18 * 256);
            }
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
}

