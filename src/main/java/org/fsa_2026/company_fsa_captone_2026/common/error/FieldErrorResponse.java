package org.fsa_2026.company_fsa_captone_2026.common.error;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FieldErrorResponse {
    String field;
    List<String> messages;
}
