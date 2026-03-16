package org.fsa_2026.company_fsa_captone_2026.base.spec;

import org.fsa_2026.company_fsa_captone_2026.base.crud.dto.FilterOperator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterField {

    String entityField() default "";

    FilterOperator operator() default FilterOperator.EQUAL;

    boolean ignoreCase() default true;
}
