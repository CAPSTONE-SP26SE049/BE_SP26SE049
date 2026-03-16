package org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.component;

import lombok.Builder;

import java.util.function.Function;

@Builder
public record ExportColumn<T>(
        String header,
        Function<T, Object> valueExtractor
) {
}

