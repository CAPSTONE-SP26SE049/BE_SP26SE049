package org.fsa_2026.company_fsa_captone_2026.entity.enums;

/**
 * Challenge Type Enum
 * Quản lý các loại challenge
 */
public enum ChallengeType {
    PRONUNCIATION("pronunciation"),
    LISTENING("listening"),
    SPEAKING("speaking"),
    READING("reading"),
    COMPREHENSION("comprehension");

    private final String value;

    ChallengeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ChallengeType fromValue(String value) {
        for (ChallengeType type : ChallengeType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown challenge type: " + value);
    }
}

