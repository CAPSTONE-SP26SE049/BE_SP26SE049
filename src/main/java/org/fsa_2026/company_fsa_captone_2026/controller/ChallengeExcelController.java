package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;
import org.fsa_2026.company_fsa_captone_2026.service.ChallengeExcelService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Controller tái sử dụng cho Import/Export Excel kho câu hỏi.
 * Pattern URL: /api/v1/excel/challenge-bank/{skillType}/...
 * Có thể mở rộng tương tự cho các entity khác: /api/v1/excel/students/...
 */
@RestController
@RequestMapping("/api/v1/excel/challenge-bank")
@RequiredArgsConstructor
@Tag(name = "Challenge Bank Excel", description = "Import/Export Excel cho kho câu hỏi — hỗ trợ 4 kỹ năng")
@SecurityRequirement(name = "bearer-jwt")
public class ChallengeExcelController {

    private final ChallengeExcelService excelService;

    // ═══════════════════════════════════════
    //  1. DOWNLOAD TEMPLATE
    // ═══════════════════════════════════════

    @GetMapping("/{skillType}/template")
    @Operation(summary = "Tải template Excel mẫu theo kỹ năng",
               description = "skillType: READING, LISTENING, WRITING, SPEAKING. File .xlsx có header + 2 dòng dữ liệu mẫu.")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String skillType) {
        SkillType skill = parseSkillType(skillType);
        byte[] file = excelService.generateTemplate(skill);

        String filename = "template_" + skill.name().toLowerCase() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    // ═══════════════════════════════════════
    //  2. IMPORT
    // ═══════════════════════════════════════

    @PostMapping(value = "/{skillType}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import câu hỏi từ file Excel",
               description = "Upload file .xlsx đã điền theo template. Check trùng bằng contentText + skillType.")
    public ResponseEntity<ApiResponse<ChallengeExcelService.ImportResult>> importExcel(
            @PathVariable String skillType,
            @RequestParam("file") MultipartFile file) {

        SkillType skill = parseSkillType(skillType);
        ChallengeExcelService.ImportResult result = excelService.importFromExcel(skill, file);

        return ResponseEntity.ok(ApiResponse.success("Import hoàn tất", result));
    }

    // ═══════════════════════════════════════
    //  3. EXPORT
    // ═══════════════════════════════════════

    @GetMapping("/export")
    @Operation(summary = "Export câu hỏi ra file Excel",
               description = "Nếu truyền ?skillType=READING thì chỉ export kỹ năng đó. Không truyền = export tất cả (mỗi kỹ năng 1 sheet).")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String skillType) {

        SkillType skill = (skillType != null && !skillType.isBlank())
                ? parseSkillType(skillType) : null;

        byte[] file = excelService.exportToExcel(skill);

        String filename = (skill != null)
                ? "challenge_bank_" + skill.name().toLowerCase() + ".xlsx"
                : "challenge_bank_all.xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    // ═══════════════════════════════════════
    //  HELPER
    // ═══════════════════════════════════════

    private SkillType parseSkillType(String value) {
        try {
            return SkillType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Kỹ năng không hợp lệ: '" + value + "'. Chấp nhận: READING, LISTENING, WRITING, SPEAKING");
        }
    }

    // ═══════════════════════════════════════
    //  4. DOWNLOAD MIXED TEMPLATE (tổng hợp 4 kỹ năng)
    // ═══════════════════════════════════════

    @GetMapping("/mixed/template")
    @Operation(summary = "Tải template Excel tổng hợp (4 kỹ năng)",
               description = "File .xlsx có 4 sheet: READING, LISTENING, WRITING, SPEAKING — mỗi sheet có header + 2 dòng dữ liệu mẫu.")
    public ResponseEntity<byte[]> downloadMixedTemplate() {
        byte[] file = excelService.generateMixedTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"template_mixed.xlsx\"")
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    // ═══════════════════════════════════════
    //  5. IMPORT CÂU HỎI VÀO QUIZ (single skill)
    // ═══════════════════════════════════════

    @PostMapping(value = "/{skillType}/import-to-quiz/{quizId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import câu hỏi từ Excel vào Quiz (1 kỹ năng)",
               description = "Upload file .xlsx → import câu hỏi vào Challenge Bank → tự động gán vào Quiz.")
    public ResponseEntity<ApiResponse<ChallengeExcelService.ImportResult>> importToQuiz(
            @PathVariable String skillType,
            @PathVariable UUID quizId,
            @RequestParam("file") MultipartFile file) {

        SkillType skill = parseSkillType(skillType);
        ChallengeExcelService.ImportResult result = excelService.importFromExcelToQuiz(skill, file, quizId);

        return ResponseEntity.ok(ApiResponse.success("Import vào quiz hoàn tất", result));
    }

    // ═══════════════════════════════════════
    //  6. IMPORT CÂU HỎI VÀO QUIZ (MIXED — tất cả kỹ năng)
    // ═══════════════════════════════════════

    @PostMapping(value = "/mixed/import-to-quiz/{quizId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import câu hỏi tổng hợp vào Quiz (MIXED)",
               description = "Upload file .xlsx có nhiều sheet (mỗi sheet 1 kỹ năng) → import tất cả vào Quiz.")
    public ResponseEntity<ApiResponse<ChallengeExcelService.ImportResult>> importMixedToQuiz(
            @PathVariable UUID quizId,
            @RequestParam("file") MultipartFile file) {

        ChallengeExcelService.ImportResult result = excelService.importFromExcelToQuiz(null, file, quizId);

        return ResponseEntity.ok(ApiResponse.success("Import tổng hợp vào quiz hoàn tất", result));
    }
}
