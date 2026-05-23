package org.fsa_2026.company_fsa_captone_2026.entity.enums;

import lombok.Getter;

@Getter
public enum EntryTestRegionCategory {
    NORTH_NL("NORTH_NL", "Miền Bắc (N/L)"),
    CENTRAL_DGIR("CENTRAL_DGIR", "Miền Trung (D/GI/R)"),
    SOUTH_TRCH("SOUTH_TRCH", "Miền Nam (TR/CH)");

    private final String code;
    private final String description;

    EntryTestRegionCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
