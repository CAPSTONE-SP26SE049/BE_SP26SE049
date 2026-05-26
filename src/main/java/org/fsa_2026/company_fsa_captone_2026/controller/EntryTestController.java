package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.EntryTestQuestionRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.EntryTestQuestionResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.EntryTestResultResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.EntryTestResult;
import org.fsa_2026.company_fsa_captone_2026.service.EntryTestService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/test")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Entry Test", description = "Entry Test APIs")
public class EntryTestController {

    private final EntryTestService entryTestService;

    // ─── User endpoints ──────────────────────────────────────────────────

    /**
     * Lấy bộ câu hỏi kiểm tra đầu vào (placement set) cho học viên đang đăng nhập.
     *
     * @param principal thông tin JWT — email user lấy từ {@link Principal#getName()}
     * @param region    (tùy chọn) miền lọc câu hỏi: {@code NORTH}, {@code CENTRAL}, {@code SOUTH};
     *                  bỏ trống → lấy {@code region} từ profile user đăng nhập; profile trống mới trộn 3 miền
     * @return danh sách câu hỏi (khoảng 10 câu) bọc trong {@link ApiResponse}
     *
     * <p><b>Note (BUG-004):</b> {@code region=HANG_NGAY} (hoặc bất kỳ giá trị khác NORTH/CENTRAL/SOUTH) → 400 ngay;
     * chỉ khi bỏ trống {@code region} mới fallback trộn 3 miền và 200 OK. Xem {@link EntryTestService#getPlacementSet}.</p>
     */
    @GetMapping("/placement-set")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get Placement Test Set")
    public ResponseEntity<ApiResponse<List<EntryTestQuestionResponse>>> getPlacementSet(
            Principal principal,
            @RequestParam(required = false) String region) {
        log.info("Fetching placement test set for user: {}, region: {}", principal.getName(), region);
        List<EntryTestQuestionResponse> placementSet = entryTestService.getPlacementSet(region, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy bộ câu hỏi kiểm tra đầu vào thành công", placementSet));
    }

    /**
     * Chẩn đoán phát âm một bước trong bài kiểm tra đầu vào (multipart: audio + questionId).
     *
     * @param questionId UUID câu hỏi trong bảng {@code entry_test_question}
     * @param audio      file âm thanh multipart — bắt buộc định dạng {@code .webm}
     * @return kết quả chẩn đoán (accuracy, feedback, isRegional, audioUrl, …)
     * @throws java.io.IOException khi đọc byte stream từ multipart
     *
     * <p><b>Note:</b> Đã bỏ {@code catch (Exception)} catch-all để {@link org.fsa_2026.company_fsa_captone_2026.exception.ResourceNotFoundException}
     * trả 404 khi {@code questionId} không tồn tại (BUG-002). Validate {@code .webm} qua
     * {@link org.fsa_2026.company_fsa_captone_2026.common.WebmAudioValidator} trong {@link EntryTestService#analyzeEntryTestStep} (BUG-001).</p>
     */
    @PostMapping(value = "/analyze-step", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Analyze Step", description = "Analyze one pronunciation attempt with Local ASR and AI")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeStep(
            @RequestParam("questionId") UUID questionId,
            @RequestParam("file") MultipartFile audio) throws java.io.IOException {
        Map<String, Object> result = entryTestService.analyzeEntryTestStep(questionId, audio);
        return ResponseEntity.ok(ApiResponse.success("Chẩn đoán bước này thành công", result));
    }

    /**
     * Hoàn tất bài kiểm tra đầu vào: lưu kết quả tổng, mở khóa level và gán lộ trình cá nhân hóa.
     *
     * @param principal   JWT — email học viên
     * @param stepResults danh sách kết quả từng bước (thường lấy từ response {@code /analyze-step})
     * @return {@link EntryTestResult} đã lưu
     *
     * <p><b>Note:</b> Service từ chối {@code stepResults} rỗng bằng 400 để tránh chia cho 0 / 500 (BUG-003).</p>
     */
    @PostMapping("/finish")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Finish Test", description = "Finalize the entry test, unlock levels, and assign learning path")
    public ResponseEntity<ApiResponse<EntryTestResult>> finishTest(
            Principal principal,
            @RequestBody List<Map<String, Object>> stepResults) {
        String email = principal.getName();
        log.info("Finishing test for user: {}", email);
        EntryTestResult finalResult = entryTestService.saveFinalResult(email, stepResults);
        return ResponseEntity
                .ok(ApiResponse.success("Lưu kết quả kiểm tra đầu vào và mở khóa lộ trình thành công", finalResult));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────

    @GetMapping("/admin/questions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get all questions")
    public ResponseEntity<ApiResponse<List<EntryTestQuestionResponse>>> getAllQuestions() {
        return ResponseEntity.ok(ApiResponse.success("OK", entryTestService.getAllQuestions()));
    }

    @PostMapping("/admin/questions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Create question")
    public ResponseEntity<ApiResponse<EntryTestQuestionResponse>> createQuestion(
            @RequestBody EntryTestQuestionRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Tạo câu hỏi thành công", entryTestService.createQuestion(request)));
    }

    @PutMapping("/admin/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Update question")
    public ResponseEntity<ApiResponse<EntryTestQuestionResponse>> updateQuestion(
            @PathVariable UUID id,
            @RequestBody EntryTestQuestionRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Cập nhật thành công", entryTestService.updateQuestion(id, request)));
    }

    @DeleteMapping("/admin/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Delete question")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable UUID id) {
        entryTestService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công", null));
    }

    @GetMapping("/admin/results")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Get all user test results")
    public ResponseEntity<ApiResponse<List<EntryTestResultResponse>>> getAllResults() {
        return ResponseEntity.ok(ApiResponse.success("OK", entryTestService.getAllResults()));
    }
}
