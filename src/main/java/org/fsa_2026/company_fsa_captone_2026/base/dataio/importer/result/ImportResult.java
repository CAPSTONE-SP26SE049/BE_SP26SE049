package org.fsa_2026.company_fsa_captone_2026.base.dataio.importer.result;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportResult {

    int successCount;
    int failureCount;
    List<RowError> errors = new ArrayList<>();
}