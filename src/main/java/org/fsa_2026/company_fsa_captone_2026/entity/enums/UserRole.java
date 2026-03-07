package org.fsa_2026.company_fsa_captone_2026.entity.enums;

/**
 * User Role Enum
 * Quản lý các loại role của user
 */
public enum UserRole {
    STUDENT("student"),
    EDUCATOR("educator"),
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromValue(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}

