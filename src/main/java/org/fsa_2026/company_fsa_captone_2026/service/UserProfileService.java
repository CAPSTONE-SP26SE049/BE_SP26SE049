package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ChangePasswordRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.UserProfileRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.UserProfileResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final AccountRepository accountRepository;
    private final AuthService authService; // to reuse getUserProfile mapping logic
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserProfileResponse updateProfile(String email, UserProfileRequest request) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "User not found"));

        if (request.getFullName() != null) {
            account.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            account.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPhone() != null) {
            account.setPhone(request.getPhone());
        }
        if (request.getRegion() != null) {
            account.setRegion(normalizeRegionForStorage(request.getRegion()));
        }

        accountRepository.save(account);

        log.info("Profile updated for user: {}", email);
        return authService.getUserProfile(email);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Không tìm thấy tài khoản người dùng"));

        if (!passwordEncoder.matches(request.getOldPassword(), account.getPasswordHash())) {
            throw new ApiException("BAD_REQUEST", "Mật khẩu cũ không chính xác");
        }

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        log.info("Password updated successfully for user: {}", email);
    }

    private String normalizeRegionForStorage(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        return switch (raw.trim().toUpperCase()) {
            case "BAC", "MIEN_BAC", "BẮC" -> "NORTH";
            case "TRUNG", "MIEN_TRUNG" -> "CENTRAL";
            case "NAM", "MIEN_NAM" -> "SOUTH";
            case "NORTH", "CENTRAL", "SOUTH" -> raw.trim().toUpperCase();
            default -> raw.trim().toUpperCase();
        };
    }
}
