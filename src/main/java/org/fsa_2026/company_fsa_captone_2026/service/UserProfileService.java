package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.UserProfileRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.UserProfileResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final AccountRepository accountRepository;
    private final AuthService authService; // to reuse getUserProfile mapping logic

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

        accountRepository.save(account);

        log.info("Profile updated for user: {}", email);
        return authService.getUserProfile(email);
    }
}
