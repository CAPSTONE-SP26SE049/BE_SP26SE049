package org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.resolver;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.metadata.ExportFieldMeta;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExportValueResolver {

    RelationResolverEngine relationEngine;

    public Object resolve(Object source, ExportFieldMeta meta) {

        Object value = readValue(source, meta);

        if (value == null)
            return "";

        if (meta.relation()) {
            return relationEngine.resolve(value, meta);
        }

        if (value instanceof Enum<?> e)
            return e.name();

        if (value instanceof LocalDateTime dt &&
                !meta.dateFormat().isBlank()) {
            return dt.format(DateTimeFormatter.ofPattern(meta.dateFormat()));
        }

        if (!meta.path().isBlank()) {
            return relationEngine.resolve(value, meta);
        }

        return value.toString();
    }

    private Object readValue(Object source, ExportFieldMeta meta) {
        try {
            return meta.field().get(source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
