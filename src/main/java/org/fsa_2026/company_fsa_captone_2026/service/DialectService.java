package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectResponse;
import org.fsa_2026.company_fsa_captone_2026.repository.DialectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialectService {

    private final DialectRepository dialectRepository;

    @Transactional(readOnly = true)
    public List<DialectResponse> getAllDialects() {
        return dialectRepository.findAll()
                .stream()
                .map(DialectResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
