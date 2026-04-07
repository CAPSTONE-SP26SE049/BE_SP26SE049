package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;
import org.fsa_2026.company_fsa_captone_2026.service.AdminChallengeBankExcelService;
import org.fsa_2026.company_fsa_captone_2026.service.AdminLevelExcelService;
import org.fsa_2026.company_fsa_captone_2026.service.AdminUserExcelService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel endpoints cho Admin: Template / Import / Export (batch).
 * Base: /api/v1/admin/excel
 */
@RestController
@RequestMapping("/api/v1/admin/excel")
@RequiredArgsConstructor
@Tag(name = "Admin Excel", description = "Template/Import/Export Excel cho Admin")
@SecurityRequirement(name = "bearer-jwt")
public class AdminExcelController {

    private final AdminUserExcelService adminUserExcelService;
    private final AdminLevelExcelService adminLevelExcelService;
    private final AdminChallengeBankExcelService adminChallengeBankExcelService;

    // =========================
    // USERS (Educators)
    // =========================

    @GetMapping("/users/template")
    @Operation(summary = "Tải template Excel tạo giáo viên",
            description = "Cột: Email, FullName")
    public ResponseEntity<byte[]> downloadUsersTemplate() {
        byte[] file = adminUserExcelService.generateTemplate();
        return asAttachment(file, "template_teachers.xlsx");
    }

    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import giáo viên từ Excel",
            description = "Map từng dòng → {email, fullName} → gọi createEducatorAccount()")
    public ResponseEntity<ApiResponse<AdminUserExcelService.ImportResult>> importUsers(
            @RequestParam("file") MultipartFile file) {
        var result = adminUserExcelService.importFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success("Import hoàn tất", result));
    }

    @GetMapping("/users/export")
    @Operation(summary = "Export danh sách giáo viên ra Excel",
            description = "Cột: Email, FullName (lọc role=EDUCATOR)")
    public ResponseEntity<byte[]> exportUsers() {
        byte[] file = adminUserExcelService.exportToExcel();
        return asAttachment(file, "teachers_export.xlsx");
    }

    // =========================
    // LEVELS
    // =========================

    @GetMapping("/levels/template")
    @Operation(summary = "Tải template Excel tạo chương học (Level)",
            description = "Cột: Tên chương học, Phương ngữ, Mô tả")
    public ResponseEntity<byte[]> downloadLevelsTemplate() {
        byte[] file = adminLevelExcelService.generateTemplate();
        return asAttachment(file, "template_levels.xlsx");
    }

    @PostMapping(value = "/levels/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import chương học từ Excel",
            description = "Map từng dòng → LevelCreateRequest (metadata_json) → gọi adminService.createLevel()")
    public ResponseEntity<ApiResponse<AdminLevelExcelService.ImportResult>> importLevels(
            @RequestParam("file") MultipartFile file) {
        var result = adminLevelExcelService.importFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success("Import hoàn tất", result));
    }

    @GetMapping("/levels/export")
    @Operation(summary = "Export danh sách chương học ra Excel",
            description = "Cột theo template; lấy từ metadataJson hiện có")
    public ResponseEntity<byte[]> exportLevels() {
        byte[] file = adminLevelExcelService.exportToExcel();
        return asAttachment(file, "levels_export.xlsx");
    }

    // =========================
    // CHALLENGE BANK (ADMIN)
    // =========================

    @GetMapping("/challenge-bank/template")
    @Operation(summary = "Tải template Excel kho câu hỏi (Admin - 1 sheet bao quát)",
            description = "Cột cố định + linh hoạt theo skillType (READING/LISTENING/WRITING/SPEAKING/ENTRY_TEST)")
    public ResponseEntity<byte[]> downloadChallengeBankTemplate() {
        byte[] file = adminChallengeBankExcelService.generateTemplate();
        return asAttachment(file, "template_challenge_bank.xlsx");
    }

    @PostMapping(value = "/challenge-bank/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import kho câu hỏi (Admin) từ Excel",
            description = "Đọc từng dòng, dựa skillType build metadataJson rồi gọi challengeBankService.createChallenge()")
    public ResponseEntity<ApiResponse<AdminChallengeBankExcelService.ImportResult>> importChallengeBank(
            @RequestParam("file") MultipartFile file) {
        var result = adminChallengeBankExcelService.importFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success("Import hoàn tất", result));
    }

    @GetMapping("/challenge-bank/export")
    @Operation(summary = "Export kho câu hỏi (Admin) ra Excel",
            description = "Dropdown FE dùng skillType=... hoặc bỏ trống để export tất cả")
    public ResponseEntity<byte[]> exportChallengeBank(@RequestParam(required = false) String skillType) {
        SkillType st = null;
        if (skillType != null && !skillType.isBlank()) {
            st = SkillType.valueOf(skillType.trim().toUpperCase());
        }
        byte[] file = adminChallengeBankExcelService.exportToExcel(st);
        String filename = st != null ? ("challenge_bank_" + st.name().toLowerCase() + ".xlsx") : "challenge_bank_all.xlsx";
        return asAttachment(file, filename);
    }

    private ResponseEntity<byte[]> asAttachment(byte[] file, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header("Access-Control-Expose-Headers", "Content-Disposition")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }
}

