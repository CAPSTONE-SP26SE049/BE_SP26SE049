package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelService {

    private final LearningUnitRepository learningUnitRepository;

    @Transactional(readOnly = true)
    public List<LevelResponse> getLevelsByDialect(String dialectId) {
        return learningUnitRepository
                .findByParentIdAndType(UUID.fromString(dialectId), "LEVEL")
                .stream()
                .map(LevelResponse::fromEntity)
                .filter(r -> r.getStatus() == null || !"REJECTED".equals(r.getStatus()))
                .sorted(Comparator.comparingInt(r -> r.getLevelOrder() != null ? r.getLevelOrder() : 0))
                .collect(Collectors.toList());
    }
}
