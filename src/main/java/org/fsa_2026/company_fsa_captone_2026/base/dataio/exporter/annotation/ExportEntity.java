package org.fsa_2026.company_fsa_captone_2026.base.dataio.exporter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExportEntity {

    String fileName() default "export";

    String sheetName() default "Sheet1";
}
