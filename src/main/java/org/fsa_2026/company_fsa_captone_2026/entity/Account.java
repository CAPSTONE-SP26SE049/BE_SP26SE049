package org.fsa_2026.company_fsa_captone_2026.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "total_stars", nullable = false)
    @Builder.Default
    private Integer totalStars = 0;

    @Column(name = "current_streak_days", nullable = false)
    @Builder.Default
    private Integer currentStreakDays = 0;

    @Column(name = "badge_count", nullable = false)
    @Builder.Default
    private Integer badgeCount = 0;

    /**
     * Date of the most recent login — used to calculate daily streak.
     * null for accounts that have never logged in after this feature was added.
     */
    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

    @Column(name = "total_experience", nullable = false)
    @Builder.Default
    private Integer totalExperience = 0;

    /**
     * Factory: tạo Account mới với role USER cho đăng ký thường
     *
     * @param email email người dùng
     * @param passwordHash mật khẩu đã băm
     * @param phone số điện thoại
     * @param region vùng miền
     * @return thực thể Account mới ở role USER
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
