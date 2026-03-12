package org.fsa_2026.company_fsa_captone_2026.common.base;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Dialect;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.DialectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DialectStandardService – Demo thực tế kế thừa AbstractCrudService
 *
 * <p>
 * Đây là <b>code mẫu chuẩn</b> để copy-paste cho các tính năng khác.
 * </p>
 *
 * <h2>Nhận xét:</h2>
 * <ul>
 * <li>Controller: cần <b>0 dòng</b> logic CRUD – kế thừa
 * BaseCrudController</li>
 * <li>Service: chỉ cần implement 5 method (getAll, getById, create, update,
 * delete)</li>
 * <li>Export Excel: <b>tự động</b> qua {@code @ExcelColumn} trên
 * {@link DialectResponse}</li>
 * <li>Import Excel: <b>chỉ cần override</b> {@code importFromExcel()} và viết
 * parse logic</li>
 * </ul>
 *
 * <h2>Cách copy sang entity khác (ví dụ Level):</h2>
 * <ol>
 * <li>Copy file này → đổi tên thành {@code LevelStandardService}</li>
 * <li>Thay {@code DialectCreateRequest} → {@code LevelCreateRequest}</li>
 * <li>Thay {@code DialectResponse} → {@code LevelResponse}</li>
 * <li>Thay {@code Dialect} → {@code Level}</li>
 * <li>Thay {@code DialectRepository} → {@code LevelRepository}</li>
 * <li>Cập nhật logic trong create/update theo nghiệp vụ Level</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialectStandardService extends AbstractCrudService<DialectCreateRequest, DialectResponse> {

    private final DialectRepository dialectRepository;

    // ==========================================
    // CRUD chuẩn
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<DialectResponse> getAll() {
        return dialectRepository.findAll().stream()
                .map(DialectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DialectResponse getById(UUID id) {
        return DialectResponse.fromEntity(
                dialectRepository.findById(id)
                        .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect với ID: " + id)));
    }

    @Override
    @Transactional
    public DialectResponse create(DialectCreateRequest request) {
        Dialect dialect = Dialect.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return DialectResponse.fromEntity(dialectRepository.save(dialect));
    }

    @Override
    @Transactional
    public DialectResponse update(UUID id, DialectCreateRequest request) {
        Dialect dialect = dialectRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy Dialect với ID: " + id));

        dialect.setName(request.getName());
        dialect.setDescription(request.getDescription());

        return DialectResponse.fromEntity(dialectRepository.save(dialect));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!dialectRepository.existsById(id)) {
            throw new ApiException("NOT_FOUND", "Không tìm thấy Dialect với ID: " + id);
        }
        dialectRepository.deleteById(id);
    }

    // ==========================================
    // Import Excel – Override từ AbstractCrudService
    // ==========================================

    /**
     * Import danh sách Dialect từ file Excel.
     *
     * <p>
     * <b>Cấu trúc file Excel mẫu:</b>
     * </p>
     * <table>
     * <tr>
     * <th>Tên vùng miền</th>
     * <th>Mô tả</th>
     * </tr>
     * <tr>
     * <td>Miền Bắc</td>
     * <td>Phát âm theo giọng Hà Nội</td>
     * </tr>
     * </table>
     *
     * <p>
     * Hàng đầu tiên (header) sẽ bị bỏ qua.
     * </p>
     */
    @Override
    @Transactional
    public List<DialectResponse> importFromExcel(MultipartFile file) throws IOException {
        List<DialectResponse> importedDialects = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Bỏ qua hàng header (hàng 0), bắt đầu từ hàng 1
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null)
                    continue;

                // Đọc cột 0 = name, cột 1 = description
                String name = getCellStringValue(row.getCell(0));
                String description = getCellStringValue(row.getCell(1));

                // Bỏ qua dòng trống
                if (name.isBlank()) {
                    log.warn("Skipping empty row at index {}", rowIndex);
                    continue;
                }

                // Tạo và lưu entity
                Dialect dialect = Dialect.builder()
                        .name(name)
                        .description(description.isBlank() ? null : description)
                        .build();

                DialectResponse saved = DialectResponse.fromEntity(dialectRepository.save(dialect));
                importedDialects.add(saved);
                log.debug("Imported dialect at row {}: name={}", rowIndex, name);
            }
        }

        log.info("Excel import completed: {} dialects imported", importedDialects.size());
        return importedDialects;
    }
}
