package org.fsa_2026.company_fsa_captone_2026.common.hash;

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
