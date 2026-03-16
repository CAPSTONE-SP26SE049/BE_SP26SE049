package org.fsa_2026.company_fsa_captone_2026.base.dataio.importer.result;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RowError {

    int rowNumber;
    String message;
}
