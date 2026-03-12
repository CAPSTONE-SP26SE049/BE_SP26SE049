package org.fsa_2026.company_fsa_captone_2026.common.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation đánh dấu column header cho Excel export.
 *
 * <p>
 * Dùng trên field của DTO Response để chỉ định tên cột trong file Excel.
 * </p>
 *
 * <pre>{@code
 * @ExcelColumn("Tên vùng miền")
 * private String name;
 *
 * @ExcelColumn("Mô tả")
 * private String description;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelColumn {
    /** Tên cột trong file Excel */
    String value() default "";

    /** Thứ tự cột (bắt đầu từ 0). -1 = theo thứ tự khai báo */
    int order() default -1;
}
