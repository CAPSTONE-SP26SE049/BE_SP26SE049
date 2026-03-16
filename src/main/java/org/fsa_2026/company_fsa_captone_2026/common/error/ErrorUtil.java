package org.fsa_2026.company_fsa_captone_2026.common.error;

import org.fsa_2026.company_fsa_captone_2026.exception.BusinessValidationException;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@UtilityClass
public class ErrorUtil {

    public void throwIfHasErrors(Map<String, List<String>> errors) {
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }
    }

    public void addError(Map<String, List<String>> errors,
                          String field,
                          String message) {
        errors.computeIfAbsent(field, k -> new ArrayList<>())
                .add(message);
    }
}
