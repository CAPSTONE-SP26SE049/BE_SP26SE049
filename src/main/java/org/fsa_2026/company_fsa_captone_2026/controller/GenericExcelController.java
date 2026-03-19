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

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/excel")
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

    @GetMapping("/export/{entityClassName}")
    @Operation(summary = "Export dữ liệu của bất kỳ Entity nào", description = "entityClassName là tên class Entity (ví dụ: ChallengeBank, Classroom, User)")
    public void exportFile(
            @PathVariable String entityClassName,
            @RequestParam(defaultValue = "EXCEL") FileFormat format,
            HttpServletResponse response
    ) throws Exception {
        Class<?> entityClass = resolveEntityClass(entityClassName);
        doExport(format, entityClass, response);
    }

    @PostMapping(value = "/import/{entityClassName}", consumes = "multipart/form-data")
    @Operation(summary = "Import dữ liệu bằng File Excel cho bất kỳ Entity nào")
    public ResponseEntity<ApiResponse<ImportResult>> importFile(
            @PathVariable String entityClassName,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        Class<?> entityClass = resolveEntityClass(entityClassName);
        ImportResult result = doImport(file, entityClass);
        return ResponseEntity.ok(ApiResponse.success("Import thành công dữ liệu " + entityClassName, result));
    }

    // --- HELPER METHODS ---

    private <T> void doExport(
            FileFormat format, 
            Class<T> entityClass, 
            HttpServletResponse response) throws Exception {
        @SuppressWarnings("unchecked")
        JpaRepository<T, ?> repository = (JpaRepository<T, ?>) repositories.getRepositoryFor(entityClass)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy JpaRepository cho Entity: " + entityClass.getSimpleName()));
        
        exportService.export(
                format,
                repository.findAll(),
                entityClass,
                response
        );
    }

    private <T> ImportResult doImport(
            MultipartFile file, 
            Class<T> entityClass) throws Exception {
        @SuppressWarnings("unchecked")
        JpaRepository<T, ?> repository = (JpaRepository<T, ?>) repositories.getRepositoryFor(entityClass)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy JpaRepository cho Entity: " + entityClass.getSimpleName()));
        
        return importService.importFile(
                file,
                entityClass,
                repository
        );
    }

    private Class<?> resolveEntityClass(String entityName) throws ClassNotFoundException {
        // Assume entities are mostly in the main entity package
        String basePackage = "org.fsa_2026.company_fsa_captone_2026.entity.";
        String normalizedName = entityName.substring(0, 1).toUpperCase() + entityName.substring(1);
        try {
            return Class.forName(basePackage + normalizedName); // Try direct map
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Thực thể " + normalizedName + " không tồn tại trong hệ thống. Vui lòng truyền đúng tên class Entity.");
        }
    }
}
