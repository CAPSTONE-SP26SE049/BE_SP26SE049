package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.repository.LevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelService {

    private final LevelRepository levelRepository;

    @Transactional(readOnly = true)
    public List<LevelResponse> getLevelsByDialect(String dialectId) {
        return levelRepository.findByDialectIdOrderByLevelOrderAsc(UUID.fromString(dialectId))
                .stream()
                .filter(level -> org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.APPROVED
                        .equals(level.getStatus()))
                .map(LevelResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
