package org.fsa_2026.company_fsa_captone_2026.common.base;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Base CRUD Controller – Template chuẩn để kế thừa
 *
 * <p>
 * Cung cấp đầy đủ 7 endpoint chuẩn:
 * </p>
 * <ul>
 * <li>{@code GET  /} – Lấy toàn bộ danh sách</li>
 * <li>{@code GET  /{id}} – Lấy chi tiết theo ID</li>
 * <li>{@code POST /} – Tạo mới</li>
 * <li>{@code PUT  /{id}} – Cập nhật</li>
 * <li>{@code DELETE /{id}} – Xóa</li>
 * <li>{@code GET  /export} – Xuất file Excel (.xlsx)</li>
 * <li>{@code POST /import} – Nhập từ file Excel (.xlsx)</li>
 * </ul>
 *
 * <h2>Cách kế thừa (copy-paste template):</h2>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;RestController
 *     &#64;RequestMapping("/api/v1/admin/content/dialects")
 *     &#64;Tag(name = "Dialect", description = "Dialect CRUD")
 *     @SecurityRequirement(name = "bearer-jwt")
 *     public class DialectController extends BaseCrudController<DialectCreateRequest, DialectResponse> {
 *
 *         public DialectController(DialectService service) {
 *             super(service, "dialects"); // "dialects" là tên file Excel khi export
 *         }
 *     }
 * }
 * </pre>
 *
 * @param <REQ> DTO Request
 * @param <RES> DTO Response
 */
@Slf4j
public abstract class BaseCrudController<REQ, RES> {

    protected final BaseCrudService<REQ, RES> service;
    protected final String resourceName;

    protected BaseCrudController(BaseCrudService<REQ, RES> service, String resourceName) {
        this.service = service;
        this.resourceName = resourceName;
    }

    // ==========================================
    // GET / – Danh sách tất cả
    // ==========================================
    @GetMapping
    @Operation(summary = "Get All", description = "Lấy toàn bộ danh sách")
    public ResponseEntity<ApiResponse<List<RES>>> getAll() {
        log.info("[{}] Getting all records", resourceName);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành công", service.getAll()));
    }

    // ==========================================
    // GET /{id} – Chi tiết
    // ==========================================
    @GetMapping("/{id}")
    @Operation(summary = "Get By ID", description = "Lấy chi tiết theo ID")
    public ResponseEntity<ApiResponse<RES>> getById(@PathVariable UUID id) {
        log.info("[{}] Getting record by id: {}", resourceName, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thành công", service.getById(id)));
    }

    // ==========================================
    // POST / – Tạo mới
    // ==========================================
    @PostMapping
    @Operation(summary = "Create", description = "Tạo mới")
    public ResponseEntity<ApiResponse<RES>> create(@Valid @RequestBody REQ request) {
        log.info("[{}] Creating new record", resourceName);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mới thành công", service.create(request)));
    }

    // ==========================================
    // PUT /{id} – Cập nhật
    // ==========================================
    @PutMapping("/{id}")
    @Operation(summary = "Update", description = "Cập nhật thông tin")
    public ResponseEntity<ApiResponse<RES>> update(
            @PathVariable UUID id,
            @Valid @RequestBody REQ request) {
        log.info("[{}] Updating record id: {}", resourceName, id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", service.update(id, request)));
    }

    // ==========================================
    // DELETE /{id} – Xóa
    // ==========================================
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete", description = "Xóa theo ID")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        log.info("[{}] Deleting record id: {}", resourceName, id);
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công", null));
    }

    // ==========================================
    // GET /export – Export Excel
    // ==========================================
    @GetMapping("/export")
    @Operation(summary = "Export Excel", description = "Xuất toàn bộ dữ liệu ra file .xlsx")
    public ResponseEntity<byte[]> exportToExcel() {
        log.info("[{}] Exporting to Excel", resourceName);
        try {
            List<RES> allData = service.getAll();
            byte[] excelBytes = service.exportToExcel(allData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(resourceName + "_export.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (IOException e) {
            log.error("[{}] Export Excel failed", resourceName, e);
            throw new ApiException("EXPORT_FAILED", "Xuất file Excel thất bại: " + e.getMessage());
        }
    }

    // ==========================================
    // POST /import – Import Excel
    // ==========================================
    @PostMapping("/import")
    @Operation(summary = "Import Excel", description = "Nhập dữ liệu từ file .xlsx")
    public ResponseEntity<ApiResponse<List<RES>>> importFromExcel(
            @RequestParam("file") MultipartFile file) {
        log.info("[{}] Importing from Excel, filename={}, size={} bytes",
                resourceName, file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            throw new ApiException("INVALID_FILE", "File Excel không được rỗng");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new ApiException("INVALID_FILE", "Chỉ hỗ trợ file .xlsx hoặc .xls");
        }

        try {
            List<RES> importedData = service.importFromExcel(file);
            return ResponseEntity.ok(ApiResponse.success(
                    "Import thành công " + importedData.size() + " bản ghi",
                    importedData));
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            log.error("[{}] Import Excel failed", resourceName, e);
            throw new ApiException("IMPORT_FAILED", "Nhập file Excel thất bại: " + e.getMessage());
        }
    }
}
