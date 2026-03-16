package org.fsa_2026.company_fsa_captone_2026.base.dataio.importer.parser;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface FileParser {

    List<Map<String, String>> parse(InputStream inputStream);

    boolean supports(String filename);
}
