package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.base.dataio.common.FileFormat;
import org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.service.ExportService;
import org.fsa_2026.company_fsa_captone_2026.base.dataio.importer.result.ImportResult;
import org.fsa_2026.company_fsa_captone_2026.base.dataio.importer.service.ImportService;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/api/v1/admin/excel")
@RequiredArgsConstructor
@Tag(name = "Generic Excel Import/Export", description = "API dùng chung cho nhiều trang để Import và Export dữ liệu Excel")
@SecurityRequirement(name = "bearer-jwt")
public class GenericExcelController {

    private final ExportService exportService;
    private final ImportService importService;
    private final ApplicationContext applicationContext;
    private Repositories repositories;

    @PostConstruct
    public void init() {
        this.repositories = new Repositories(applicationContext);
    }

    @GetMapping("/{entityClassName}/export")
    @Operation(summary = "Export dữ liệu của bất kỳ Entity nào", description = "entityClassName là tên class Entity (ví dụ: ChallengeBank, Classroom, User)")
    public void exportFile(
            @PathVariable String entityClassName,
            @RequestParam(defaultValue = "EXCEL") FileFormat format,
            HttpServletResponse response
    ) throws Exception {
        Class<?> entityClass = resolveEntityClass(entityClassName);
        invokeExport(entityClass, format, response);
    }

    @PostMapping(value = "/{entityClassName}/import", consumes = "multipart/form-data")
    @Operation(summary = "Import dữ liệu bằng File Excel cho bất kỳ Entity nào")
    public ResponseEntity<ApiResponse<ImportResult>> importFile(
            @PathVariable String entityClassName,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        Class<?> entityClass = resolveEntityClass(entityClassName);
        ImportResult result = invokeImport(entityClass, file);
        return ResponseEntity.ok(ApiResponse.success("Import thành công dữ liệu " + entityClassName, result));
    }

    @GetMapping("/{entityClassName}/template")
    @Operation(summary = "Tải file template Excel cho Entity")
    public void getTemplate(
            @PathVariable String entityClassName,
            HttpServletResponse response
    ) throws Exception {
        Class<?> entityClass = resolveEntityClass(entityClassName);
        invokeTemplate(entityClass, response);
    }

    // --- GENERIC HELPERS TO CAPTURE CAPTURE#1 of ? ---

    @SuppressWarnings("unchecked")
    private <T> void invokeExport(Class<T> entityClass, FileFormat format, HttpServletResponse response) throws Exception {
        JpaRepository<T, ?> repository = (JpaRepository<T, ?>) getRepository(entityClass);
        exportService.export(
                format,
                repository.findAll(),
                entityClass,
                response
        );
    }

    @SuppressWarnings("unchecked")
    private <T, ID> ImportResult invokeImport(Class<T> entityClass, MultipartFile file) {
        JpaRepository<T, ID> repository = (JpaRepository<T, ID>) getRepository(entityClass);
        return importService.importFile(
                file,
                entityClass,
                repository
        );
    }

    @SuppressWarnings("unchecked")
    private <T> void invokeTemplate(Class<T> entityClass, HttpServletResponse response) throws Exception {
        exportService.export(
                FileFormat.EXCEL,
                java.util.List.of(),
                entityClass,
                response
        );
    }

    private JpaRepository<?, ?> getRepository(Class<?> entityClass) {
        return (JpaRepository<?, ?>) repositories.getRepositoryFor(entityClass)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy JpaRepository cho Entity: " + entityClass.getSimpleName()));
    }

    private Class<?> resolveEntityClass(String entityName) {
        String basePackage = "org.fsa_2026.company_fsa_captone_2026.entity.";
        
        // Handle aliases
        String className = switch (entityName.toLowerCase()) {
            case "users", "user", "accounts", "account" -> "Account";
            case "levels", "level" -> "LearningUnit"; // Levels are stored in LearningUnit table
            case "challenges", "challenge", "challenge-bank" -> "ChallengeBank";
            case "classrooms", "classroom" -> "Classroom";
            default -> entityName.substring(0, 1).toUpperCase() + entityName.substring(1);
        };

        try {
            return Class.forName(basePackage + className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Thực thể " + className + " không tồn tại trong hệ thống. Vui lòng truyền đúng tên class Entity.");
        }
    }
}
