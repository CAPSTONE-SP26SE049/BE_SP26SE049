package org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.component;

import lombok.Builder;

import java.util.List;

@Builder
public record ExportSheetConfig<T>(
        String sheetName,
        List<ExportColumn<T>> columns
) {
}
