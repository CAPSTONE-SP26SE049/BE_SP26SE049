package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class LevelService {

    private final LearningUnitRepository learningUnitRepository;
    private final org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository accountLearningUnitRepository;
    private final AccountRepository accountRepository;
    private final org.fsa_2026.company_fsa_captone_2026.repository.CustomLearningPathRepository customLearningPathRepository;

    @Transactional(readOnly = true)
    public List<LevelResponse> getLevelsByDialect(String dialectId) {
        UUID parentId = UUID.fromString(dialectId);
        List<LearningUnit> allLevels = new java.util.ArrayList<>();
        fetchAllDescendantLevels(parentId, allLevels);
        
        return allLevels.stream()
                .map(LevelResponse::fromEntity)
                .filter(r -> r.getStatus() == null || (!"REJECTED".equals(r.getStatus()) && !"DELETED".equals(r.getStatus())))
                .sorted(Comparator.comparingInt(r -> r.getLevelOrder() != null ? r.getLevelOrder() : 0))
                .collect(Collectors.toList());
    }

    private void fetchAllDescendantLevels(UUID parentId, List<LearningUnit> accumulator) {
        // Direct children that are levels (Optimized: removes N+1 recursive calls for Quizzes/Sub-units)
        List<LearningUnit> children = learningUnitRepository.findByParentId(parentId);
        for (LearningUnit child : children) {
            if ("LEVEL".equals(child.getType())) {
                accumulator.add(child);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<LevelResponse> getUserRoadmap(String email, String dialectId, String type) {
        Account account = accountRepository.findByEmail(email).orElse(null);
        if (account == null) return List.of();

        // If explicitly requested custom path, or if no dialect is provided and user HAS an active custom path
        if ("custom".equalsIgnoreCase(type)) {
            var activePath = customLearningPathRepository.findFirstByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(account.getId());
            if (activePath.isPresent()) {
                log.info("Returning personalized roadmap for user {}", email);
                return getCustomPathLevels(activePath.get(), account);
            }
        }
        
        if (dialectId != null && !dialectId.isEmpty()) {
            try {
                return getLevelsWithProgress(dialectId, account);
            } catch (Exception e) {
                log.warn("Invalid dialectId provided: {}", dialectId);
            }
        }

        // Fallback: return all levels if no dialectId specified or invalid
        List<LevelResponse> levels = learningUnitRepository.findByType("LEVEL")
                .stream()
                .map(LevelResponse::fromEntity)
                .filter(r -> r.getStatus() == null || (!"REJECTED".equals(r.getStatus()) && !"DELETED".equals(r.getStatus())))
                .sorted(java.util.Comparator.comparingInt(r -> r.getLevelOrder() != null ? r.getLevelOrder() : 0))
                .collect(java.util.stream.Collectors.toList());
                
        if (account != null) {
             // We can't use getLevelsWithProgress easily here because it expects a dialectId
             // But for fallback we just return basic levels for now or implement global progress
        }
        
        return levels;
    }

    private List<LevelResponse> getCustomPathLevels(org.fsa_2026.company_fsa_captone_2026.entity.CustomLearningPath activePath, Account account) {
        List<LevelResponse> levels = activePath.getLevels().stream()
                .sorted(Comparator.comparingInt(org.fsa_2026.company_fsa_captone_2026.entity.CustomPathLevel::getOrderIndex))
                .map(pl -> {
                    LevelResponse resp = LevelResponse.fromEntity(pl.getLevel());
                    resp.setLevelOrder(pl.getOrderIndex() + 1); // Display as 1-based index
                    return resp;
                })
                .collect(Collectors.toList());

        return populateProgressAndUnlocking(levels, account);
    }

    private List<LevelResponse> populateProgressAndUnlocking(List<LevelResponse> levels, Account account) {
        List<org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit> progressList =
                accountLearningUnitRepository.findByAccountIdWithLearningUnit(account.getId());

        java.util.Map<UUID, org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit> progressMap = progressList.stream()
                .collect(Collectors.toMap(
                        al -> al.getLearningUnit().getId(),
                        al -> al,
                        (existing, replacement) -> existing
                ));

        boolean previousCompleted = true; // First level in any roadmap is unlocked

        for (LevelResponse level : levels) {
            org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit progress =
                    progressMap.get(UUID.fromString(level.getId()));

            if (progress != null) {
                level.setStarsEarned(progress.getStarsEarned());
                level.setIsCompleted(progress.getIsCompleted());
            }

            level.setIsLocked(!previousCompleted);
            previousCompleted = level.getIsCompleted() != null && level.getIsCompleted();
        }

        return levels;
    }

    @Transactional(readOnly = true)
    public List<LevelResponse> getLevelsWithProgress(String dialectId, org.fsa_2026.company_fsa_captone_2026.entity.Account account) {
        List<LevelResponse> levels = getLevelsByDialect(dialectId);
        return populateProgressAndUnlocking(levels, account);
    }
}
