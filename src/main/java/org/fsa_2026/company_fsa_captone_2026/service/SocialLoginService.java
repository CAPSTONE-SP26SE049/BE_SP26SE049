package org.fsa_2026.company_fsa_captone_2026.service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

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
import org.springframework.web.client.RestTemplate;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Social Login Service
 * Handles Google and Facebook OAuth2 login:
 * 1. Verify token with provider (Google ID Token / Facebook Access Token)
 * 2. Find or create Account
 * 3. Generate JWT tokens
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final String META_PICTURE = "picture";

    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService; // for updateLoginStreak

    @Value("${social.google.client-id}")
    private String googleClientId;

    /**
     * Entry point for social login
     *
     * @param request social login request including provider and token
     * @return login response containing access token, refresh token and user info
     */
    @Transactional
    public LoginResponse socialLogin(SocialLoginRequest request) {
        return switch (request.getProvider().toUpperCase()) {
            case PROVIDER_GOOGLE -> loginWithGoogle(request.getToken());
            case "FACEBOOK" -> loginWithFacebook(request.getToken());
            default -> throw new ApiException("BAD_REQUEST",
                    "Provider không hỗ trợ: " + request.getProvider());
        };
    }

    /**
     * Verify Google Token (ID Token or Access Token) and login/register user
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private LoginResponse loginWithGoogle(String token) {
        // Step 1: Try verifying as ID Token (only works if FE sends id_token, not
        // access_token)
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(token);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get(META_PICTURE);
                String googleId = payload.getSubject();

                log.info("Google login verified via ID Token for: {} (googleId: {})", email, googleId);
                return findOrCreateAccountAndLogin(email, name, pictureUrl, PROVIDER_GOOGLE);
            }
            log.info("Token is not a valid ID Token (returned null), will try as Access Token");
        } catch (Exception e) {
            // Catches GeneralSecurityException, IOException, IllegalArgumentException, etc.
            // Access tokens (ya29.xxx) are opaque strings - NOT JWTs, so they fail ID Token
            // parsing.
            // This is expected behavior - fall through to Step 2 (UserInfo API).
            log.info("ID Token verification failed [{}]: {}, trying as Access Token...",
                    e.getClass().getSimpleName(), e.getMessage());
        }

        // Step 2: Treat as Access Token → call Google UserInfo API
        try {
            log.info("Calling Google UserInfo API with access token (length={})...",
                    token != null ? token.length() : 0);
            RestTemplate restTemplate = new RestTemplate();
            String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                    userInfoUrl, org.springframework.http.HttpMethod.GET, entity, Map.class);

            Map<String, Object> googleUser = response.getBody();

            if (googleUser == null || googleUser.get("sub") == null) {
                log.error("Google UserInfo API returned null or no sub field. Response: {}", googleUser);
                throw new ApiException("UNAUTHORIZED", "Google token không hợp lệ");
            }

            String email = (String) googleUser.get("email");
            String name = (String) googleUser.get("name");
            String pictureUrl = (String) googleUser.get(META_PICTURE);
            String googleId = (String) googleUser.get("sub");

            log.info("Google login verified via Access Token for: {} (googleId: {})", email, googleId);
            return findOrCreateAccountAndLogin(email, name, pictureUrl, PROVIDER_GOOGLE);

        } catch (ApiException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Log exact Google API error (e.g. 401 invalid_token)
            log.error("Google UserInfo API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException("UNAUTHORIZED",
                    "Google token không hợp lệ (lỗi " + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString());
        } catch (org.springframework.web.client.RestClientException | ClassCastException e) {
            log.error("Google Access Token network/parse error:", e);
            throw new ApiException("UNAUTHORIZED", "Không thể xác minh tài khoản Google: " +
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    /**
     * Verify Facebook Access Token and login/register user
     * Uses Facebook Graph API to get user info
     */
    @SuppressWarnings("unchecked")
    private LoginResponse loginWithFacebook(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Call Facebook Graph API to verify token and get user info
            String url = "https://graph.facebook.com/me?fields=id,name,email,picture.type(large)"
                    + "&access_token=" + accessToken;

            Map<String, Object> fbUser = restTemplate.getForObject(url, Map.class);

            if (fbUser == null || fbUser.get("id") == null) {
                throw new ApiException("UNAUTHORIZED", "Facebook token không hợp lệ");
            }

            String email = (String) fbUser.get("email");
            String name = (String) fbUser.get("name");
            String fbId = (String) fbUser.get("id");

            // Extract picture URL from nested response
            String pictureUrl = null;
            Map<String, Object> picture = (Map<String, Object>) fbUser.get(META_PICTURE);
            if (picture != null) {
                Map<String, Object> data = (Map<String, Object>) picture.get("data");
                if (data != null) {
                    pictureUrl = (String) data.get("url");
                }
            }

            // Facebook may not return email if user didn't grant permission
            if (email == null || email.isBlank()) {
                email = fbId + "@facebook.com";
                log.warn("Facebook user {} did not provide email, using fallback: {}", fbId, email);
            }

            log.info("Facebook login verified for: {} (fbId: {})", email, fbId);

            return findOrCreateAccountAndLogin(email, name, pictureUrl, "FACEBOOK");

        } catch (ApiException e) {
            throw e;
        } catch (org.springframework.web.client.RestClientException | ClassCastException e) {
            log.error("Facebook login verification failed:", e);
            throw new ApiException("UNAUTHORIZED", "Không thể xác minh tài khoản Facebook: " + e.getMessage());
        }
    }

    /**
     * Find existing account by email or create new one, then generate JWT tokens
     */
    private LoginResponse findOrCreateAccountAndLogin(
            String email, String name, String pictureUrl,
            String provider) {

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
                    .emailVerified(true) // Social login = email already verified by provider
                    .build();
            account = accountRepository.save(account);
            log.info("Tạo tài khoản mới từ {}: email={}", provider, email);
        } else {
            // Existing account - always update avatar and name from social provider
            boolean updated = false;
            if (pictureUrl != null && !pictureUrl.equals(account.getAvatarUrl())) {
                account.setAvatarUrl(pictureUrl);
                updated = true;
            }
            if (name != null && !name.isBlank() && !name.equals(account.getFullName())) {
                account.setFullName(name);
                updated = true;
            }
            if (updated) {
                account = accountRepository.save(account);
                log.info("Cập nhật thông tin từ {}: avatar={}, name={}", provider, pictureUrl, name);
            }
            log.info("Đăng nhập {} với tài khoản hiện có: email={}", provider, email);
        }

        // ── Update login streak (shared logic in AuthService) ──────────────────
        authService.updateLoginStreak(account);
        account = accountRepository.save(account);

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

        log.info("Social login [{}]: email={}, role={}, streak={} days",
                provider, email, account.getRoleCode().name(), account.getCurrentStreakDays());

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
                        .currentStreakDays(account.getCurrentStreakDays())
                        .build())
                .build();
    }
}
