package org.fsa_2026.company_fsa_captone_2026.base.dataio.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileFormat {

    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("csv", "text/csv"),
    PDF("pdf", "application/pdf");

    private final String extension;
    private final String contentType;
}
