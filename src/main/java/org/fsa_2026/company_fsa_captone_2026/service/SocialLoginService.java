package org.fsa_2026.company_fsa_captone_2026.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.JwtTokenProvider;
import org.fsa_2026.company_fsa_captone_2026.dto.LoginResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.SocialLoginRequest;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.RefreshToken;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;

/**
 * Social Login Service
 * Handles Google OAuth2 login:
 * 1. Verify ID Token with Google
 * 2. Find or create Account
 * 3. Generate JWT tokens
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${social.google.client-id}")
    private String googleClientId;

    /**
     * Entry point for social login
     */
    @Transactional
    public LoginResponse socialLogin(SocialLoginRequest request) {
        return switch (request.getProvider().toUpperCase()) {
            case "GOOGLE" -> loginWithGoogle(request.getToken());
            default -> throw new ApiException("BAD_REQUEST",
                    "Provider không hỗ trợ: " + request.getProvider());
        };
    }

    /**
     * Verify Google ID Token and login/register user
     */
    private LoginResponse loginWithGoogle(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new ApiException("UNAUTHORIZED", "Google token không hợp lệ hoặc đã hết hạn");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");
            String googleId = payload.getSubject();

            log.info("Google login verified for: {} (googleId: {})", email, googleId);

            return findOrCreateAccountAndLogin(email, name, pictureUrl, "GOOGLE", googleId);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google login verification failed:", e);
            throw new ApiException("UNAUTHORIZED", "Không thể xác minh tài khoản Google: " + e.getMessage());
        }
    }

    /**
     * Find existing account by email or create new one, then generate JWT tokens
     */
    private LoginResponse findOrCreateAccountAndLogin(
            String email, String name, String pictureUrl,
            String provider, String providerId) {

        Account account = accountRepository.findByEmail(email).orElse(null);

        if (account == null) {
            // Create new account - auto verified, no password needed
            account = Account.builder()
                    .email(email)
                    .passwordHash("SOCIAL_LOGIN_" + provider) // Placeholder, cannot be used for normal login
                    .roleCode(RoleCode.USER)
                    .fullName(name != null ? name : email.split("@")[0])
                    .avatarUrl(pictureUrl)
                    .isActive(true)
                    .emailVerified(true) // Social login = email already verified by Google
                    .build();
            account = accountRepository.save(account);
            log.info("Tạo tài khoản mới từ {}: email={}", provider, email);
        } else {
            // Existing account - update avatar if needed
            if (account.getAvatarUrl() == null && pictureUrl != null) {
                account.setAvatarUrl(pictureUrl);
                account = accountRepository.save(account);
            }
            log.info("Đăng nhập {} với tài khoản hiện có: email={}", provider, email);
        }

        // Generate JWT tokens (same logic as normal login)
        String accessToken = jwtTokenProvider.generateAccessToken(
                account.getId(), account.getEmail(), account.getRoleCode().name());

        // Revoke old refresh tokens
        refreshTokenRepository.revokeAllByAccountId(account.getId());

        // Create new refresh token
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .account(account)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("Social login thành công [{}]: email={}, role={}", provider, email, account.getRoleCode().name());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .user(LoginResponse.UserInfo.builder()
                        .id(account.getId().toString())
                        .email(account.getEmail())
                        .fullName(account.getFullName())
                        .role(account.getRoleCode().name())
                        .region(account.getRegion())
                        .avatar(account.getAvatarUrl())
                        .build())
                .build();
    }
}
