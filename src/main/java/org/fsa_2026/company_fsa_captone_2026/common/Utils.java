package org.fsa_2026.company_fsa_captone_2026.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Utility Class - Tiện ích bổ trợ
 * Chứa các helper methods dùng chung trong ứng dụng
 */
@Slf4j
public class Utils {

    private static final String EMAIL_PATTERN =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    private static final String PHONE_PATTERN =
            "^(\\+\\d{1,3}[- ]?)?\\d{9,15}$";

    private static final Pattern EMAIL_REGEX = Pattern.compile(EMAIL_PATTERN);
    private static final Pattern PHONE_REGEX = Pattern.compile(PHONE_PATTERN);

    /**
     * Kiểm tra email có hợp lệ hay không
     */
    public static boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return EMAIL_REGEX.matcher(email).matches();
    }

    /**
     * Kiểm tra số điện thoại có hợp lệ hay không
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return PHONE_REGEX.matcher(phone).matches();
    }

    /**
     * Kiểm tra string có trống hay không
     */
    public static boolean isEmpty(String str) {
        return !StringUtils.hasText(str);
    }

    /**
     * Kiểm tra collection có trống hay không
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Kiểm tra object có null hay không
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * Kiểm tra object có không null hay không
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /**
     * Xử lý exception và log error
     */
    public static void logError(String message, Exception e) {
        log.error(message, e);
    }

    /**
     * Xử lý exception và log error với custom message
     */
    public static void logError(String message, Exception e, String customMessage) {
        log.error("{} - {}", message, customMessage, e);
    }

    /**
     * Log debug message
     */
    public static void logDebug(String message) {
        log.debug(message);
    }

    /**
     * Log debug message with parameters
     */
    public static void logDebug(String message, Object... params) {
        log.debug(message, params);
    }

    /**
     * Log info message
     */
    public static void logInfo(String message) {
        log.info(message);
    }

    /**
     * Log info message with parameters
     */
    public static void logInfo(String message, Object... params) {
        log.info(message, params);
    }

    /**
     * Chuẩn hóa username (lowercase, trim)
     */
    public static String normalizeUsername(String username) {
        return StringUtils.hasText(username) ? username.trim().toLowerCase() : null;
    }

    /**
     * Chuẩn hóa email (lowercase, trim)
     */
    public static String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase() : null;
    }

    /**
     * Kiểm tra mật khẩu có độ mạnh tối thiểu hay không
     * Yêu cầu: ít nhất 8 ký tự, có chữ hoa, chữ thường, số
     */
    public static boolean isStrongPassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            return false;
        }

        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        return hasUpperCase && hasLowerCase && hasDigit;
    }
}
