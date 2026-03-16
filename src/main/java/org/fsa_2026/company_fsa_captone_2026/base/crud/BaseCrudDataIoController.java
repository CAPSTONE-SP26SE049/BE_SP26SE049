package org.fsa_2026.company_fsa_captone_2026.base.crud;

import org.fsa_2026.company_fsa_captone_2026.base.dataio.common.FileFormat;
import org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.service.ExportService;
import org.fsa_2026.company_fsa_captone_2026.base.dataio.importer.result.ImportResult;
import org.fsa_2026.company_fsa_captone_2026.base.dataio.importer.service.ImportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public abstract class BaseCrudDataIoController<E, I, D, F>
        extends BaseCrudController<I, D, F> {

    @Autowired
    protected ExportService exportService;

    @Autowired
    protected ImportService importService;

    protected abstract BaseCrudRepository<E, I> getRepository();

    protected abstract Class<E> getEntityClass();

    @GetMapping("/export")
    public void exportFile(
            @RequestParam(defaultValue = "EXCEL") FileFormat format,
            HttpServletResponse response
    ) throws IOException {
        exportService.export(
                format,
                getRepository().findAll(),
                getEntityClass(),
                response
        );
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ImportResult importFile(
            @RequestParam("file") MultipartFile file
    ) {
        return importService.importFile(
                file,
                getEntityClass(),
                getRepository()
        );
    }
}
