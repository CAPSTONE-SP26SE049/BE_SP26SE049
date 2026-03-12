package org.fsa_2026.company_fsa_captone_2026.common.base;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DialectStandardController – Demo kế thừa BaseCrudController
 *
 * <p>
 * Đây là <b>code mẫu chuẩn</b> để copy-paste cho các tính năng khác.
 * </p>
 *
 * <p>
 * Chỉ cần 10 dòng để có đầy đủ <b>7 endpoint</b>:
 * </p>
 * <ul>
 * <li>{@code GET    /api/v1/base/dialects} – Lấy danh sách</li>
 * <li>{@code GET    /api/v1/base/dialects/{id}} – Lấy chi tiết</li>
 * <li>{@code POST   /api/v1/base/dialects} – Tạo mới</li>
 * <li>{@code PUT    /api/v1/base/dialects/{id}} – Cập nhật</li>
 * <li>{@code DELETE /api/v1/base/dialects/{id}} – Xóa</li>
 * <li>{@code GET    /api/v1/base/dialects/export} – Xuất Excel</li>
 * <li>{@code POST   /api/v1/base/dialects/import} – Nhập Excel</li>
 * </ul>
 *
 * <h2>Cách copy sang entity khác (ví dụ Level):</h2>
 * <ol>
 * <li>Copy file này → đổi tên {@code LevelStandardController}</li>
 * <li>Thay {@code DialectCreateRequest} → {@code LevelCreateRequest}</li>
 * <li>Thay {@code DialectResponse} → {@code LevelResponse}</li>
 * <li>Thay {@code DialectStandardService} → {@code LevelStandardService}</li>
 * <li>Đổi {@code @RequestMapping} URL</li>
 * <li>Đổi {@code super(service, "dialects")} →
 * {@code super(service, "levels")}</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/base/dialects")
@Tag(name = "Base CRUD Demo – Dialect", description = "Demo đầy đủ CRUD + Import/Export Excel theo chuẩn Base")
@SecurityRequirement(name = "bearer-jwt")
public class DialectStandardController extends BaseCrudController<DialectCreateRequest, DialectResponse> {

    public DialectStandardController(DialectStandardService service) {
        // "dialects" = tên file khi người dùng download Excel: "dialects_export.xlsx"
        super(service, "dialects");
    }

    // ======================================================
    // KHÔNG cần thêm gì nữa!
    // Tất cả 7 endpoints đã được kế thừa từ BaseCrudController
    // ======================================================
}
