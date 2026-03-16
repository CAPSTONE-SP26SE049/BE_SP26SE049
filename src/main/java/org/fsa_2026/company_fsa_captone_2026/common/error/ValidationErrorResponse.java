package org.fsa_2026.company_fsa_captone_2026.common.error;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationErrorResponse {

    String message;
    Map<String, List<String>> errors;
}
