package org.fsa_2026.company_fsa_captone_2026.base.dataio.importer.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ImportHash {

    boolean hashDefault() default true;
}
