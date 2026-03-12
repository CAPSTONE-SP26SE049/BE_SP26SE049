package org.fsa_2026.company_fsa_captone_2026.base;

import org.fsa_2026.company_fsa_captone_2026.common.base.DialectStandardService;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Dialect;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.DialectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BaseCrudServiceTest – Unit test template chuẩn cho Service Layer
 *
 * <p>
 * Đây là <b>test template chuẩn</b> để copy-paste khi tạo service mới.
 * </p>
 *
 * <h2>Cách copy sang entity khác (ví dụ Level):</h2>
 * <ol>
 * <li>Copy file này → đổi tên {@code LevelServiceTest}</li>
 * <li>Thay {@code DialectStandardService} → {@code LevelStandardService}</li>
 * <li>Thay {@code DialectRepository} → {@code LevelRepository}</li>
 * <li>Thay {@code DialectCreateRequest, DialectResponse, Dialect} → Level tương
 * ứng</li>
 * <li>Cập nhật mock data theo field của Level</li>
 * </ol>
 *
 * <p>
 * <b>Framework:</b> JUnit 5 + Mockito (không cần Spring Context → chạy rất
 * nhanh)
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DialectStandardService – Unit Tests")
class BaseCrudServiceTest {

    // ──────────────── Mock dependencies ────────────────
    @Mock
    private DialectRepository dialectRepository;

    @InjectMocks
    private DialectStandardService service;

    // ──────────────── Test fixtures ────────────────
    private UUID testId;
    private Dialect testDialect;
    private DialectCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        testDialect = new Dialect();
        testDialect.setId(testId);
        testDialect.setName("Miền Bắc");
        testDialect.setDescription("Phát âm theo giọng Hà Nội");

        createRequest = DialectCreateRequest.builder()
                .name("Miền Bắc")
                .description("Phát âm theo giọng Hà Nội")
                .build();
    }

    // ==========================================
    // GET ALL
    // ==========================================

    @Test
    @DisplayName("getAll: trả về danh sách đầy đủ")
    void testGetAll_returnsListSuccessfully() {
        // GIVEN
        when(dialectRepository.findAll()).thenReturn(List.of(testDialect));

        // WHEN
        List<DialectResponse> result = service.getAll();

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Miền Bắc");
        verify(dialectRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll: trả về list rỗng nếu DB rỗng")
    void testGetAll_returnsEmptyList() {
        when(dialectRepository.findAll()).thenReturn(List.of());
        List<DialectResponse> result = service.getAll();
        assertThat(result).isEmpty();
    }

    // ==========================================
    // GET BY ID
    // ==========================================

    @Test
    @DisplayName("getById: tìm thấy → trả về đúng DTO")
    void testGetById_found() {
        when(dialectRepository.findById(testId)).thenReturn(Optional.of(testDialect));

        DialectResponse result = service.getById(testId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testId.toString());
        assertThat(result.getName()).isEqualTo("Miền Bắc");
    }

    @Test
    @DisplayName("getById: không tìm thấy → ném ApiException NOT_FOUND")
    void testGetById_notFound_throwsException() {
        when(dialectRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Không tìm thấy Dialect");
    }

    // ==========================================
    // CREATE
    // ==========================================

    @Test
    @DisplayName("create: lưu entity và trả về DTO")
    void testCreate_success() {
        when(dialectRepository.save(any(Dialect.class))).thenReturn(testDialect);

        DialectResponse result = service.create(createRequest);

        assertThat(result.getName()).isEqualTo("Miền Bắc");
        verify(dialectRepository, times(1)).save(any(Dialect.class));
    }

    // ==========================================
    // UPDATE
    // ==========================================

    @Test
    @DisplayName("update: cập nhật thành công → trả về DTO mới")
    void testUpdate_success() {
        when(dialectRepository.findById(testId)).thenReturn(Optional.of(testDialect));
        when(dialectRepository.save(any(Dialect.class))).thenReturn(testDialect);

        DialectCreateRequest updateRequest = DialectCreateRequest.builder()
                .name("Miền Nam")
                .description("Phát âm theo giọng Sài Gòn")
                .build();

        DialectResponse result = service.update(testId, updateRequest);

        assertThat(result).isNotNull();
        verify(dialectRepository, times(1)).save(any(Dialect.class));
    }

    @Test
    @DisplayName("update: ID không tồn tại → ném ApiException")
    void testUpdate_notFound_throwsException() {
        when(dialectRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), createRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Không tìm thấy Dialect");
    }

    // ==========================================
    // DELETE
    // ==========================================

    @Test
    @DisplayName("delete: xóa thành công khi ID tồn tại")
    void testDelete_success() {
        when(dialectRepository.existsById(testId)).thenReturn(true);

        service.delete(testId);

        verify(dialectRepository, times(1)).deleteById(testId);
    }

    @Test
    @DisplayName("delete: ID không tồn tại → ném ApiException")
    void testDelete_notFound_throwsException() {
        when(dialectRepository.existsById(any())).thenReturn(false);

        assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Không tìm thấy Dialect");

        verify(dialectRepository, never()).deleteById(any());
    }

    // ==========================================
    // EXPORT EXCEL
    // ==========================================

    @Test
    @DisplayName("exportToExcel: trả về mảng byte hợp lệ")
    void testExportToExcel_returnsBytes() throws IOException {
        List<DialectResponse> data = List.of(
                DialectResponse.builder().id(testId.toString()).name("Miền Bắc").description("HN").build());

        byte[] result = service.exportToExcel(data);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("exportToExcel: list rỗng → trả về Excel rỗng (không crash)")
    void testExportToExcel_emptyInput_returnsEmptyExcel() throws IOException {
        byte[] result = service.exportToExcel(List.of());
        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0); // Vẫn là file Excel hợp lệ
    }
}
