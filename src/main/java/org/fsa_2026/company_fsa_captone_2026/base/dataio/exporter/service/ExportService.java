package org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.service;

import jakarta.servlet.http.HttpServletResponse;
import org.fsa_2026.company_fsa_captone_2026.base.dataio.common.FileFormat;

import java.io.IOException;
import java.util.List;

public interface ExportService {

    <T> void export(
            FileFormat format,
            List<T> data,
            Class<T> clazz,
            HttpServletResponse response
    ) throws IOException;
}
