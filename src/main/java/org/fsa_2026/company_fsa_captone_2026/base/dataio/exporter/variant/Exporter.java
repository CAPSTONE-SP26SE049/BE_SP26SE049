package org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.variant;



import org.fsa_2026.company_fsa_captone_2026.base.dataio.common.FileFormat;
import org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.component.ExportSheetConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface Exporter {

    FileFormat format();

    <T> void export(
            List<T> data,
            ExportSheetConfig<T> config,
            OutputStream os
    ) throws IOException;
}


