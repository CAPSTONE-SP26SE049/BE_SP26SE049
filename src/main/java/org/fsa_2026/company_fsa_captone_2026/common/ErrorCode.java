package org.fsa_2026.company_fsa_captone_2026.common;

/**
 * Error Code Enum - Mã lỗi chuẩn hóa cho API
 */
public enum ErrorCode {

    // Thành công
    SUCCESS("0000", "Thành công"),

    // Lỗi chung
    INVALID_INPUT("1001", "Tham số đầu vào không hợp lệ"),
    RESOURCE_NOT_FOUND("1002", "Không tìm thấy tài nguyên"),
    INTERNAL_SERVER_ERROR("1003", "Lỗi hệ thống nội bộ"),
    UNAUTHORIZED("1004", "Không có quyền truy cập"),
    FORBIDDEN("1005", "Truy cập bị từ chối"),
    BAD_REQUEST("1006", "Yêu cầu không hợp lệ"),

    // Xác thực & Phân quyền
    INVALID_JWT_TOKEN("2001", "Token JWT không hợp lệ"),
    EXPIRED_JWT_TOKEN("2002", "Token JWT đã hết hạn"),
    MISSING_JWT_TOKEN("2003", "Thiếu token JWT"),
    UNSUPPORTED_JWT_TOKEN("2004", "Token JWT không được hỗ trợ"),
    AUTHENTICATION_FAILED("2005", "Xác thực thất bại"),
    INSUFFICIENT_PERMISSIONS("2006", "Không đủ quyền hạn"),

    // Người dùng
    USER_NOT_FOUND("3001", "Không tìm thấy người dùng"),
    USER_ALREADY_EXISTS("3002", "Người dùng đã tồn tại"),
    INVALID_CREDENTIALS("3003", "Email hoặc mật khẩu không đúng"),
    ACCOUNT_DISABLED("3004", "Tài khoản đã bị vô hiệu hóa"),

    // Lỗi xác thực dữ liệu
    VALIDATION_FAILED("4001", "Xác thực dữ liệu thất bại"),
    DUPLICATE_ENTRY("4002", "Dữ liệu bị trùng lặp"),
    INVALID_FORMAT("4003", "Định dạng không hợp lệ");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
