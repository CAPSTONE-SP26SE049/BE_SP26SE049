package org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.metadata;

import lombok.Builder;

import java.lang.reflect.Field;

@Builder
public record ExportFieldMeta(
        String header,
        Field field,
        String dateFormat,
        String path,
        boolean relation,
        String separator
) {
}
