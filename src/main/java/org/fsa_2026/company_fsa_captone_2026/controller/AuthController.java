package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.*;
import org.fsa_2026.company_fsa_captone_2026.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles register, login, refresh token operations
 * Base: /api/v1/auth
 */
@Slf4j
@RestController
@RequestMapping(Constants.AUTH_PREFIX)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API endpoints")
public class AuthController {

        private final AuthService authService;

        /**
         * Register - POST /api/v1/auth/register
         */
        @PostMapping("/register")
        @Operation(summary = "Register", description = "Create a new user account")
        public ResponseEntity<ApiResponse<RegisterResponse>> register(
                        @Valid @RequestBody RegisterRequest request) {

                log.info("Register attempt for email: {}", request.getEmail());

                RegisterResponse response = authService.register(request);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success(
                                                "Đăng ký tài khoản thành công. Vui lòng kiểm tra email để xác thực.",
                                                response));
        }

        /**
         * Create Admin - POST /api/v1/auth/create-admin
         * Secret endpoint for creating an initial admin account
         */
        @PostMapping("/create-admin")
        @Operation(summary = "Create Admin", description = "Create a new admin account (bootstrap)")
        public ResponseEntity<ApiResponse<RegisterResponse>> createAdmin(
                        @Valid @RequestBody AdminCreateRequest request) {

                log.info("Admin creation attempt for email: {}", request.getEmail());

                RegisterResponse response = authService.createAdmin(request);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(ApiResponse.success("Tạo tài khoản quản trị viên thành công.", response));
        }

        /**
         * Xác thực email - POST /api/v1/auth/verify-email
         * Xác nhận mã OTP gửi qua email sau khi đăng ký
         */
        @PostMapping("/verify-email")
        @Operation(summary = "Xác thực email", description = "Xác nhận mã OTP gửi qua email sau khi đăng ký")
        public ResponseEntity<ApiResponse<Void>> verifyEmail(
                        @Valid @RequestBody VerifyEmailRequest request) {

                log.info("Verify email attempt for: {}", request.getEmail());

                authService.verifyEmail(request.getEmail(), request.getCode());

                return ResponseEntity
                                .ok(ApiResponse.success("Xác thực email thành công. Bạn có thể đăng nhập ngay bây giờ",
                                                null));
        }

        /**
         * Gửi lại mã xác thực - POST /api/v1/auth/resend-verification
         */
        @PostMapping("/resend-verification")
        @Operation(summary = "Gửi lại mã xác thực", description = "Gửi lại mã OTP xác thực email")
        public ResponseEntity<ApiResponse<Void>> resendVerification(
                        @Valid @RequestBody ForgotPasswordRequest request) {

                log.info("Resend verification for: {}", request.getEmail());

                authService.resendVerificationEmail(request.getEmail());

                return ResponseEntity
                                .ok(ApiResponse.success("Mã xác thực mới đã được gửi đến email của bạn", null));
        }

        /**
         * Login - POST /api/v1/auth/login
         */
        @PostMapping("/login")
        @Operation(summary = "Login", description = "Authenticate user and get JWT tokens")
        public ResponseEntity<ApiResponse<LoginResponse>> login(
                        @Valid @RequestBody LoginRequest request) {

                log.info("Login attempt for email: {}", request.getEmail());

                LoginResponse response = authService.login(request);

                return ResponseEntity
                                .ok(ApiResponse.success("Đăng nhập thành công", response));
        }

        /**
         * Refresh Token - POST /api/v1/auth/refresh
         */
        @PostMapping("/refresh")
        @Operation(summary = "Refresh Token", description = "Get a new access token using a valid refresh token")
        public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
                        @Valid @RequestBody RefreshTokenRequest request) {

                log.info("Token refresh attempt");

                RefreshTokenResponse response = authService.refreshToken(request.getRefreshToken());

                return ResponseEntity
                                .ok(ApiResponse.success("Làm mới token thành công", response));
        }

        /**
         * Quên mật khẩu - POST /api/v1/auth/forgot-password
         * Gửi mã OTP 6 số qua email
         */
        @PostMapping("/forgot-password")
        @Operation(summary = "Quên mật khẩu", description = "Gửi mã xác nhận qua email để đặt lại mật khẩu")
        public ResponseEntity<ApiResponse<Void>> forgotPassword(
                        @Valid @RequestBody ForgotPasswordRequest request) {

                log.info("Forgot password for email: {}", request.getEmail());

                authService.forgotPassword(request.getEmail());

                return ResponseEntity
                                .ok(ApiResponse.success("Mã xác nhận đã được gửi đến email của bạn", null));
        }

        /**
         * Đặt lại mật khẩu - POST /api/v1/auth/reset-password
         * Xác nhận mã OTP và đặt mật khẩu mới
         */
        @PostMapping("/reset-password")
        @Operation(summary = "Đặt lại mật khẩu", description = "Xác nhận mã OTP và đặt mật khẩu mới")
        public ResponseEntity<ApiResponse<Void>> resetPassword(
                        @Valid @RequestBody ResetPasswordRequest request) {

                log.info("Reset password for email: {}", request.getEmail());

                authService.resetPassword(request.getEmail(), request.getResetCode(), request.getNewPassword());

                return ResponseEntity
                                .ok(ApiResponse.success("Đặt lại mật khẩu thành công", null));
        }

        /**
         * Logout - POST /api/v1/auth/logout
         * Revokes the provided refresh token
         */
        @PostMapping("/logout")
        @Operation(summary = "Logout", description = "Logout user and revoke their refresh token")
        public ResponseEntity<ApiResponse<Void>> logout(
                        @Valid @RequestBody LogoutRequest request) {

                log.info("Logout attempt");

                authService.logout(request.getRefreshToken());

                return ResponseEntity
                                .ok(ApiResponse.success("Đăng xuất thành công", null));
        }
}
