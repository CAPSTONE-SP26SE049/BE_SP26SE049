package org.fsa_2026.company_fsa_captone_2026.common;

/**
 * Application Constants
 * Chứa tất cả các hằng số được sử dụng trong ứng dụng
 */
public class Constants {

    // API Endpoints
    public static final String API_PREFIX = "/api/v1";
    public static final String AUTH_PREFIX = API_PREFIX + "/auth";
    public static final String ADMIN_PREFIX = API_PREFIX + "/admin";
    public static final String PUBLIC_PREFIX = API_PREFIX + "/public";
    public static final String USER_PREFIX = API_PREFIX + "/users";

    // JWT Constants
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_TYPE = "Bearer";

    // Default values
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String DEFAULT_PAGE_NUMBER = "0";

    // Error Messages
    public static final String INVALID_JWT_TOKEN = "Invalid JWT token";
    public static final String EXPIRED_JWT_TOKEN = "JWT token expired";
    public static final String UNSUPPORTED_JWT_TOKEN = "Unsupported JWT token";
    public static final String JWT_CLAIMS_STRING_EMPTY = "JWT claims string is empty";

    // Time Constants
    public static final long JWT_TOKEN_VALIDITY = 86400000L; // 24 hours in milliseconds
}
