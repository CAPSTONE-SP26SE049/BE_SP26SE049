package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
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
        UUID parentId = UUID.fromString(dialectId);
        List<LearningUnit> allLevels = new java.util.ArrayList<>();
        fetchAllDescendantLevels(parentId, allLevels);
        
        return allLevels.stream()
                .map(LevelResponse::fromEntity)
                .filter(r -> r.getStatus() == null || !"REJECTED".equals(r.getStatus()))
                .sorted(Comparator.comparingInt(r -> r.getLevelOrder() != null ? r.getLevelOrder() : 0))
                .collect(Collectors.toList());
    }

    private void fetchAllDescendantLevels(UUID parentId, List<LearningUnit> accumulator) {
        // Direct children that are levels
        List<LearningUnit> children = learningUnitRepository.findByParentId(parentId);
        for (LearningUnit child : children) {
            if ("LEVEL".equals(child.getType())) {
                accumulator.add(child);
            }
            // Continue searching recursively in this child's subtree
            fetchAllDescendantLevels(child.getId(), accumulator);
        }
    }

    @Transactional(readOnly = true)
    public List<LevelResponse> getUserRoadmap(String email, String dialectId) {
        if (dialectId != null && !dialectId.isEmpty()) {
            try {
                // Return levels that are children of this specific dialectId
                return getLevelsByDialect(dialectId);
            } catch (Exception e) {
                log.warn("Invalid dialectId provided: {}", dialectId);
            }
        }

        // Fallback: return all levels if no dialectId specified or invalid
        return learningUnitRepository.findByType("LEVEL")
                .stream()
                .map(LevelResponse::fromEntity)
                .filter(r -> r.getStatus() == null || !"REJECTED".equals(r.getStatus()))
                .sorted(Comparator.comparingInt(r -> r.getLevelOrder() != null ? r.getLevelOrder() : 0))
                .collect(Collectors.toList());
    }
}
