package org.fsa_2026.company_fsa_captone_2026.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fsa_2026.company_fsa_captone_2026.common.base.DialectStandardController;
import org.fsa_2026.company_fsa_captone_2026.common.base.DialectStandardService;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectResponse;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BaseCrudControllerTest – Controller test template chuẩn với MockMvc
 *
 * <p>
 * Đây là <b>test template chuẩn</b> để copy-paste khi tạo controller mới.
 * </p>
 *
 * <h2>Cách copy sang entity khác (ví dụ Level):</h2>
 * <ol>
 * <li>Copy file này → đổi tên {@code LevelControllerTest}</li>
 * <li>Thay {@code DialectStandardController} →
 * {@code LevelStandardController}</li>
 * <li>Thay {@code DialectStandardService} → {@code LevelStandardService}</li>
 * <li>Thay {@code DialectCreateRequest}, {@code DialectResponse} → Level tương
 * ứng</li>
 * <li>Đổi URL {@code /api/v1/base/dialects} → URL mới</li>
 * </ol>
 *
 * <p>
 * <b>Framework:</b> MockMvc (standalone – không cần DB, không cần Spring
 * Security)
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DialectStandardController – MockMvc Tests")
class BaseCrudControllerTest {

    // ──────────────── Setup ────────────────
    @Mock
    private DialectStandardService service;

    @InjectMocks
    private DialectStandardController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "/api/v1/base/dialects";

    private MockMvc buildMockMvc() {
        return MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()) // Bắt lỗi chuẩn
                .build();
    }

    // ──────────────── Test fixtures ────────────────
    private DialectResponse sampleResponse() {
        return DialectResponse.builder()
                .id(UUID.randomUUID().toString())
                .name("Miền Bắc")
                .description("Phát âm theo giọng Hà Nội")
                .build();
    }

    // ==========================================
    // GET / – Danh sách
    // ==========================================

    @Test
    @DisplayName("GET /: trả về 200 + danh sách")
    void testGetAll_returns200WithList() throws Exception {
        when(service.getAll()).thenReturn(List.of(sampleResponse()));

        buildMockMvc().perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Miền Bắc"));
    }

    // ==========================================
    // GET /{id} – Chi tiết
    // ==========================================

    @Test
    @DisplayName("GET /{id}: tìm thấy → 200 + DTO")
    void testGetById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        DialectResponse response = sampleResponse();
        when(service.getById(id)).thenReturn(response);

        buildMockMvc().perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.name").value("Miền Bắc"));
    }

    @Test
    @DisplayName("GET /{id}: không tìm thấy → 404")
    void testGetById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getById(id)).thenThrow(new ApiException("NOT_FOUND", "Không tìm thấy Dialect"));

        buildMockMvc().perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // ==========================================
    // POST / – Tạo mới
    // ==========================================

    @Test
    @DisplayName("POST /: tạo mới → 201 Created")
    void testCreate_returns201() throws Exception {
        DialectCreateRequest request = DialectCreateRequest.builder()
                .name("Miền Nam")
                .description("Giọng Sài Gòn")
                .build();

        when(service.create(any(DialectCreateRequest.class))).thenReturn(sampleResponse());

        buildMockMvc().perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("POST /: tên rỗng → 400 Bad Request (validation)")
    void testCreate_missingName_returns400() throws Exception {
        DialectCreateRequest invalidRequest = DialectCreateRequest.builder()
                .name("") // Tên rỗng → @NotBlank fail
                .build();

        buildMockMvc().perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // PUT /{id} – Cập nhật
    // ==========================================

    @Test
    @DisplayName("PUT /{id}: cập nhật thành công → 200")
    void testUpdate_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        DialectCreateRequest request = DialectCreateRequest.builder()
                .name("Miền Trung")
                .description("Giọng Huế")
                .build();

        when(service.update(eq(id), any(DialectCreateRequest.class))).thenReturn(sampleResponse());

        buildMockMvc().perform(put(BASE_URL + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    // ==========================================
    // DELETE /{id} – Xóa
    // ==========================================

    @Test
    @DisplayName("DELETE /{id}: xóa thành công → 200")
    void testDelete_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).delete(id);

        buildMockMvc().perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa thành công"));
    }

    @Test
    @DisplayName("DELETE /{id}: không tìm thấy → 404")
    void testDelete_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ApiException("NOT_FOUND", "Không tìm thấy Dialect"))
                .when(service).delete(id);

        buildMockMvc().perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // GET /export – Excel Export
    // ==========================================

    @Test
    @DisplayName("GET /export: trả về 200 + Content-Type Excel")
    void testExportExcel_returns200WithExcelContentType() throws Exception {
        when(service.getAll()).thenReturn(List.of(sampleResponse()));
        when(service.exportToExcel(any())).thenReturn(new byte[] { 1, 2, 3, 4 }); // mock bytes

        buildMockMvc().perform(get(BASE_URL + "/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
