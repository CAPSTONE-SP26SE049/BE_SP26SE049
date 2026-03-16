package com.example.starter_project_2025.system.common.hash;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BCryptHashService implements HashService {

    PasswordEncoder passwordEncoder;

    @Override
    public String hash(String raw) {
        return passwordEncoder.encode(raw);
    }
}
