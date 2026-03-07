package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;

import java.time.Instant;

/**
 * Account Entity - User Accounts (ISO Compliant)
 * Renamed from 'users' to 'account' (singular, ISO/IEC 11179-5)
 */
@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 20)
    @Builder.Default
    private RoleCode roleCode = RoleCode.USER;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "region", length = 50)
    private String region;

    // Removed nativeLanguage and targetLanguage

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "native_dialect_id")
    private Dialect nativeDialect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_dialect_id")
    private Dialect targetDialect;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verify_code", length = 6)
    private String emailVerifyCode;

    @Column(name = "email_verify_expires_at")
    private Instant emailVerifyExpiresAt;

    @Column(name = "reset_code", length = 6)
    private String resetCode;

    @Column(name = "reset_expires_at")
    private Instant resetExpiresAt;

    // Relationships
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile userProfile;

    /**
     * Factory: tạo Account mới với role USER cho đăng ký thường
     */
    public static Account createUserAccount(String email, String passwordHash, String phone, String region) {
        return Account.builder()
                .email(email)
                .passwordHash(passwordHash)
                .roleCode(RoleCode.USER)
                .phone(phone)
                .region(region)
                .isActive(true)
                .build();
    }
}
