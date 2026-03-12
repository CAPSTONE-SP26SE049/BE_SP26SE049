package org.fsa_2026.company_fsa_captone_2026.common.base;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Abstract CRUD Service – Base chuẩn để kế thừa
 *
 * <p>
 * Provides a full CRUD + Excel import/export implementation.
 * Subclasses only need to implement business-specific methods.
 * </p>
 *
 * <h2>Cách kế thừa (copy-paste template):</h2>
 * 
 * <pre>
 * {@code
 * &#64;Service
 * &#64;RequiredArgsConstructor
 * public class DialectService extends AbstractCrudService<DialectCreateRequest, DialectResponse> {
 *
 *     private final DialectRepository repository;
 *
 *     &#64;Override public List<DialectResponse> getAll() { ... }
 *     &#64;Override public DialectResponse getById(UUID id) { ... }
 *     &#64;Override public DialectResponse create(DialectCreateRequest req) { ... }
 *     &#64;Override public DialectResponse update(UUID id, DialectCreateRequest req) { ... }
 *     @Override public void delete(UUID id) { ... }
 *
 *     // Excel: chỉ cần implement importFromExcel() nếu cần custom parse
 *     // exportToExcel() đã được implement tự động qua @ExcelColumn annotation
 * }
 * }
 * </pre>
 *
 * @param <REQ> DTO Request (Create/Update)
 * @param <RES> DTO Response (phải có @ExcelColumn trên field để auto export)
 */
@Slf4j
public abstract class AbstractCrudService<REQ, RES> implements BaseCrudService<REQ, RES> {

    // ==========================================
    // Abstract methods – Subclass bắt buộc implement
    // ==========================================

    @Override
    public abstract List<RES> getAll();

    @Override
    public abstract RES getById(UUID id);

    @Override
    public abstract RES create(REQ request);

    @Override
    public abstract RES update(UUID id, REQ request);

    @Override
    public abstract void delete(UUID id);

    /**
     * Import từ Excel – mặc định throw UnsupportedOperation.
     * Override trong subclass nếu cần.
     */
    @Override
    public List<RES> importFromExcel(MultipartFile file) throws IOException {
        throw new ApiException("NOT_SUPPORTED", "Tính năng import Excel chưa được cấu hình cho resource này");
    }

    // ==========================================
    // Export Excel – Auto từ @ExcelColumn annotation
    // ==========================================

    /**
     * Xuất danh sách data sang file Excel (.xlsx).
     *
     * <p>
     * Tự động đọc các field có annotation {@link ExcelColumn} trên class RES
     * để tạo header và điền dữ liệu.
     * </p>
     *
     * @param data danh sách DTO Response cần export
     * @return mảng bytes file Excel
     */
    @Override
    public byte[] exportToExcel(List<RES> data) throws IOException {
        if (data == null || data.isEmpty()) {
            return createEmptyExcel();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");

            // Lấy danh sách field có @ExcelColumn (sort theo order)
            List<Field> excelFields = getExcelFields(data.get(0).getClass());

            if (excelFields.isEmpty()) {
                log.warn("No @ExcelColumn annotations found on class: {}", data.get(0).getClass().getSimpleName());
                return createEmptyExcel();
            }

            // ── Header Row ──
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < excelFields.size(); i++) {
                ExcelColumn annotation = excelFields.get(i).getAnnotation(ExcelColumn.class);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(annotation.value().isEmpty() ? excelFields.get(i).getName() : annotation.value());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // ── Data Rows ──
            int rowIndex = 1;
            for (RES item : data) {
                Row dataRow = sheet.createRow(rowIndex++);
                for (int colIndex = 0; colIndex < excelFields.size(); colIndex++) {
                    Field field = excelFields.get(colIndex);
                    field.setAccessible(true);
                    try {
                        Object value = field.get(item);
                        Cell cell = dataRow.createCell(colIndex);
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        } else {
                            cell.setCellValue("");
                        }
                    } catch (IllegalAccessException e) {
                        log.error("Cannot access field: {}", field.getName(), e);
                    }
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ==========================================
    // Protected helpers - có thể override trong subclass
    // ==========================================

    /**
     * Tạo style cho hàng header trong Excel.
     * Override để tùy chỉnh màu sắc/font.
     */
    protected CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Background màu xanh nhạt
        style.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Font trắng, đậm
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        // Border
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * Đọc giá trị string từ một ô Excel (xử lý cả số, string, blank).
     */
    protected String getCellStringValue(Cell cell) {
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    // ==========================================
    // Private helpers
    // ==========================================

    private List<Field> getExcelFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        // Lấy cả field từ superclass
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Arrays.stream(current.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(ExcelColumn.class))
                    .forEach(fields::add);
            current = current.getSuperclass();
        }
        // Sort theo order nếu được chỉ định
        fields.sort(Comparator.comparingInt(f -> {
            ExcelColumn ann = f.getAnnotation(ExcelColumn.class);
            return ann.order() == -1 ? Integer.MAX_VALUE : ann.order();
        }));
        return fields;
    }

    private byte[] createEmptyExcel() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Empty");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
