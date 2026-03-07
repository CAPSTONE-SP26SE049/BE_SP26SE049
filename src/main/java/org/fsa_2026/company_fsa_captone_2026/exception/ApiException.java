package org.fsa_2026.company_fsa_captone_2026.exception;

import org.fsa_2026.company_fsa_captone_2026.common.ErrorCode;

/**
 * Custom API Exception
 * Dùng để throw khi có lỗi trong business logic
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final String message;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.message = customMessage;
    }

    public ApiException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

