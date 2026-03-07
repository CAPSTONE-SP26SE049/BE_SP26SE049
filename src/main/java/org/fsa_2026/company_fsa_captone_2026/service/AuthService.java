package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.JwtTokenProvider;
import org.fsa_2026.company_fsa_captone_2026.dto.*;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.RefreshToken;
import org.fsa_2026.company_fsa_captone_2026.entity.UserProfile;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.RefreshTokenRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.UserProfileRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Service
 * Handles registration, login, token refresh business logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    /**
     * Register a new user account
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Validate uniqueness
        Map<String, String> errors = new HashMap<>();

        if (accountRepository.existsByEmail(request.getEmail())) {
            errors.put("email", "Email đã tồn tại");
        }
        if (accountRepository.existsByPhone(request.getPhone())) {
            errors.put("phone", "Số điện thoại đã tồn tại");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Xác thực dữ liệu thất bại", errors);
        }

        // Tạo mã OTP 6 số để xác thực email
        String verifyCode = generateOtpCode();

        // Create Account với role USER (đăng ký thường)
        Account account = Account.createUserAccount(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhone(),
                request.getRegion());
        // Set OTP ngay trước khi save lần đầu (tránh save 2 lần)
        account.setEmailVerifyCode(verifyCode);
        account.setEmailVerifyExpiresAt(Instant.now().plusSeconds(900)); // 15 phút

        account = accountRepository.save(account);

        // Create UserProfile
        UserProfile userProfile = UserProfile.builder()
                .account(account)
                .fullName(request.getFullName())
                .avatarUrl(generateDefaultAvatar(request.getFullName()))
                .build();

        userProfileRepository.save(userProfile);

        log.info("Đăng ký thành công: email={}, role={}", request.getEmail(), account.getRoleCode().name());

        // Build response TRƯỚC khi gửi email (đảm bảo response không bị block)
        RegisterResponse response = RegisterResponse.builder()
                .id(account.getId().toString())
                .email(account.getEmail())
                .fullName(request.getFullName())
                .role(account.getRoleCode().name())
                .build();

        // Gửi email xác thực (async - không block response, không ảnh hưởng
        // transaction)
        try {
            emailService.sendEmailVerification(request.getEmail(), request.getFullName(), verifyCode);
        } catch (Exception e) {
            // @Async method: exception thường không tới đây, nhưng phòng trường hợp proxy
            // lỗi
            log.error("Lỗi khi gửi email xác thực đến: {} - {}", request.getEmail(), e.getMessage(), e);
        }

        return response;
    }

    /**
     * Create a new Admin account
     */
    @Transactional
    public RegisterResponse createAdmin(AdminCreateRequest request) {
        // Validate uniqueness
        Map<String, String> errors = new HashMap<>();

        if (accountRepository.existsByEmail(request.getEmail())) {
            errors.put("email", "Email đã tồn tại");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Xác thực dữ liệu thất bại", errors);
        }

        // Create Account với role ADMIN
        Account account = Account.createUserAccount(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                null,
                null);

        // Set properties specifically for Admin
        account.setRoleCode(org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode.ADMIN);
        account.setEmailVerified(true); // Always verified

        account = accountRepository.save(account);

        // Create UserProfile
        UserProfile userProfile = UserProfile.builder()
                .account(account)
                .fullName(request.getFullName())
                .avatarUrl(generateDefaultAvatar(request.getFullName()))
                .build();

        userProfileRepository.save(userProfile);

        log.info("Khởi tạo Admin thành công: email={}", request.getEmail());

        return RegisterResponse.builder()
                .id(account.getId().toString())
                .email(account.getEmail())
                .fullName(request.getFullName())
                .role(account.getRoleCode().name())
                .build();
    }

    /**
     * Authenticate user and return tokens
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            // Authenticate with Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new ApiException("UNAUTHORIZED", "Email hoặc mật khẩu không đúng");
        }

        // Load account
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("UNAUTHORIZED", "Email hoặc mật khẩu không đúng"));

        // Kiểm tra email đã xác thực chưa
        if (!account.getEmailVerified()) {
            throw new ApiException("FORBIDDEN",
                    "Email chưa được xác thực. Vui lòng kiểm tra hộp thư để xác nhận email");
        }

        // Load profile
        UserProfile profile = userProfileRepository.findByAccountId(account.getId())
                .orElse(null);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                account.getId(), account.getEmail(), account.getRoleCode().name());

        // Revoke existing refresh tokens and create new one
        refreshTokenRepository.revokeAllByAccountId(account.getId());

        String refreshTokenStr = jwtTokenProvider.generateRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .account(account)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("User logged in successfully: {}", request.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .user(LoginResponse.UserInfo.builder()
                        .id(account.getId().toString())
                        .email(account.getEmail())
                        .fullName(profile != null ? profile.getFullName() : null)
                        .role(account.getRoleCode().name())
                        .region(account.getRegion())
                        .avatar(profile != null ? profile.getAvatarUrl() : null)
                        .build())
                .build();
    }

    /**
     * Refresh access token using refresh token with rotation
     */
    @Transactional
    public RefreshTokenResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new ApiException("UNAUTHORIZED", "Refresh token không hợp lệ"));

        if (!refreshToken.isUsable()) {
            throw new ApiException("UNAUTHORIZED", "Refresh token đã hết hạn hoặc bị thu hồi");
        }

        Account account = refreshToken.getAccount();

        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                account.getId(), account.getEmail(), account.getRoleCode().name());

        // Rotate refresh token: revoke old, create new
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        String newRefreshTokenStr = jwtTokenProvider.generateRefreshToken();
        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newRefreshTokenStr)
                .account(account)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(newRefreshToken);

        log.info("Token refreshed for user: {}", account.getEmail());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenStr)
                .build();
    }

    /**
     * Logout user by revoking their refresh token
     */
    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.info("Token revoked during logout for user: {}", refreshToken.getAccount().getEmail());
        });
    }

    /**
     * Get user profile by email
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy người dùng"));

        UserProfile profile = userProfileRepository.findByAccountId(account.getId())
                .orElse(null);

        return UserProfileResponse.builder()
                .id(account.getId().toString())
                .email(account.getEmail())
                .fullName(profile != null ? profile.getFullName() : null)
                .phone(account.getPhone())
                .region(account.getRegion())
                .role(account.getRoleCode().name())
                .avatar(profile != null ? profile.getAvatarUrl() : null)
                .totalStars(profile != null ? profile.getTotalStars() : 0)
                .currentStreakDays(profile != null ? profile.getCurrentStreakDays() : 0)
                .totalExperience(profile != null ? profile.getTotalExperience() : 0)
                .createdAt(account.getCreatedAt())
                .build();
    }

    /**
     * Generate DiceBear avatar URL from name
     */
    private String generateDefaultAvatar(String fullName) {
        String seed = fullName != null ? fullName.replaceAll("\\s+", "+") : "default";
        return "https://api.dicebear.com/7.x/initials/svg?seed=" + seed;
    }

    /**
     * Tạo mã OTP 6 số ngẫu nhiên
     */
    private String generateOtpCode() {
        return String.format("%06d", new java.security.SecureRandom().nextInt(999999));
    }

    /**
     * Xác thực email bằng mã OTP
     */
    @Transactional
    public void verifyEmail(String email, String code) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy tài khoản với email này"));

        if (account.getEmailVerified()) {
            throw new ApiException("BAD_REQUEST", "Email đã được xác thực trước đó");
        }

        if (account.getEmailVerifyCode() == null || !account.getEmailVerifyCode().equals(code)) {
            throw new ApiException("BAD_REQUEST", "Mã xác nhận không đúng");
        }

        if (account.getEmailVerifyExpiresAt() == null || Instant.now().isAfter(account.getEmailVerifyExpiresAt())) {
            throw new ApiException("BAD_REQUEST", "Mã xác nhận đã hết hạn. Vui lòng yêu cầu gửi lại mã mới");
        }

        // Xác thực thành công
        account.setEmailVerified(true);
        account.setEmailVerifyCode(null);
        account.setEmailVerifyExpiresAt(null);
        accountRepository.save(account);

        log.info("Xác thực email thành công cho: {}", email);
    }

    /**
     * Gửi lại mã xác thực email
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy tài khoản với email này"));

        if (account.getEmailVerified()) {
            throw new ApiException("BAD_REQUEST", "Email đã được xác thực trước đó");
        }

        // Tạo mã OTP mới
        String verifyCode = generateOtpCode();
        account.setEmailVerifyCode(verifyCode);
        account.setEmailVerifyExpiresAt(Instant.now().plusSeconds(900)); // 15 phút
        accountRepository.save(account);

        // Lấy tên người dùng
        String fullName = userProfileRepository.findByAccountId(account.getId())
                .map(UserProfile::getFullName)
                .orElse("Người dùng");

        // Gửi email (async)
        try {
            emailService.sendEmailVerification(email, fullName, verifyCode);
            log.info("Đã gửi lại mã xác thực email đến: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi gửi lại email xác thực đến: {} - {}", email, e.getMessage(), e);
        }
    }

    /**
     * Quên mật khẩu - gửi mã OTP 6 số qua email
     */
    @Transactional
    public void forgotPassword(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy tài khoản với email này"));

        // Tạo mã OTP 6 số
        String resetCode = generateOtpCode();

        // Lưu mã và thời gian hết hạn (15 phút)
        account.setResetCode(resetCode);
        account.setResetExpiresAt(Instant.now().plusSeconds(900)); // 15 phút
        accountRepository.save(account);

        // Lấy tên người dùng
        String fullName = userProfileRepository.findByAccountId(account.getId())
                .map(UserProfile::getFullName)
                .orElse("Người dùng");

        // Gửi email (async) — wrap try-catch để đảm bảo lỗi SMTP không gây 500
        try {
            emailService.sendResetPasswordEmail(email, fullName, resetCode);
            log.info("Đã gửi mã đặt lại mật khẩu đến: {}", email);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email đặt lại mật khẩu đến: {} - {}", email, e.getMessage(), e);
            // Không throw — OTP đã lưu DB, user có thể dùng resend
        }
    }

    /**
     * Đặt lại mật khẩu bằng mã OTP
     */
    @Transactional
    public void resetPassword(String email, String resetCode, String newPassword) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy tài khoản với email này"));

        // Kiểm tra mã
        if (account.getResetCode() == null || !account.getResetCode().equals(resetCode)) {
            throw new ApiException("BAD_REQUEST", "Mã xác nhận không đúng");
        }

        // Kiểm tra hết hạn
        if (account.getResetExpiresAt() == null || Instant.now().isAfter(account.getResetExpiresAt())) {
            throw new ApiException("BAD_REQUEST", "Mã xác nhận đã hết hạn");
        }

        // Cập nhật mật khẩu mới
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setResetCode(null);
        account.setResetExpiresAt(null);
        accountRepository.save(account);

        log.info("Đặt lại mật khẩu thành công cho: {}", email);
    }

    /**
     * Custom ValidationException for registration errors
     */
    public static class ValidationException extends RuntimeException {
        private final Map<String, String> errors;

        public ValidationException(String message, Map<String, String> errors) {
            super(message);
            this.errors = errors;
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }
}
