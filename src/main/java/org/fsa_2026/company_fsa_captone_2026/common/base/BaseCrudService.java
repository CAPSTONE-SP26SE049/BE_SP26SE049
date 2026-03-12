package org.fsa_2026.company_fsa_captone_2026.common.base;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Base CRUD Service Interface
 *
 * <p>
 * Generic interface cho tất cả Service có nghiệp vụ CRUD + Import/Export Excel.
 * </p>
 *
 * <p>
 * <b>Cách dùng (copy-paste template):</b>
 * </p>
 * 
 * <pre>{@code
 * // 1. Service kế thừa AbstractCrudService thay vì implement interface này
 * // trực tiếp
 * @Service
 * public class DialectService extends AbstractCrudService<Dialect, DialectCreateRequest, DialectResponse> {
 *     // chỉ cần override toRow() + fromRow()
 * }
 * }</pre>
 *
 * @param <REQ> DTO Request (Create/Update)
 * @param <RES> DTO Response
 */
public interface BaseCrudService<REQ, RES> {

    /** Lấy toàn bộ danh sách */
    List<RES> getAll();

    /** Lấy chi tiết theo ID */
    RES getById(UUID id);

    /** Tạo mới */
    RES create(REQ request);

    /** Cập nhật */
    RES update(UUID id, REQ request);

    /** Xóa */
    void delete(UUID id);

    /**
     * Xuất danh sách ra file Excel (.xlsx).
     *
     * @return mảng bytes của file Excel
     * @throws IOException nếu có lỗi ghi file
     */
    byte[] exportToExcel(List<RES> data) throws IOException;

    /**
     * Import danh sách từ file Excel (.xlsx).
     *
     * @param file MultipartFile .xlsx được upload
     * @return danh sách bản ghi đã được lưu vào DB
     * @throws IOException nếu có lỗi đọc file
     */
    List<RES> importFromExcel(MultipartFile file) throws IOException;
}
