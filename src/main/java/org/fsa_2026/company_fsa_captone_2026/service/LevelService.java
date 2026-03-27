package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.LevelResponse;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelService {

    private final LearningUnitRepository learningUnitRepository;
    private final org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository accountLearningUnitRepository;
    private final org.fsa_2026.company_fsa_captone_2026.repository.ContentItemRepository contentItemRepository;

    @Transactional(readOnly = true)
    public List<LevelResponse> getLevelsByDialect(String dialectId) {
        return learningUnitRepository
                .findByParentIdAndType(UUID.fromString(dialectId),
                        org.fsa_2026.company_fsa_captone_2026.common.Constants.TYPE_LEVEL)
                .stream()
                .map(LevelResponse::fromEntity)
                .filter(r -> r.getStatus() == null || !"REJECTED".equals(r.getStatus()))
                .sorted(java.util.Comparator.comparingInt(r -> r.getLevelOrder() != null ? r.getLevelOrder() : 0))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LevelResponse> getLevelsWithProgress(String dialectId,
            org.fsa_2026.company_fsa_captone_2026.entity.Account account) {
        List<LevelResponse> levels = getLevelsByDialect(dialectId);
        if (levels.isEmpty())
            return levels;

        List<UUID> levelIds = levels.stream().map(l -> UUID.fromString(l.getId()))
                .collect(java.util.stream.Collectors.toList());

        // Batch fetch all quizzes for all levels in this dialect
        List<org.fsa_2026.company_fsa_captone_2026.entity.ContentItem> allModernQuizzes = contentItemRepository
                .findByLearningUnitIdInAndType(levelIds, "QUIZ");
        List<org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit> allLegacyQuizzes = learningUnitRepository
                .findByParentIdInAndType(levelIds, "QUIZ");

        // Fetch user progress
        java.util.List<org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit> progressList = accountLearningUnitRepository
                .findByAccountId(account.getId());

        java.util.Map<UUID, org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit> progressMap = progressList
                .stream()
                .filter(al -> al.getLearningUnit() != null)
                .collect(java.util.stream.Collectors.toMap(
                        al -> al.getLearningUnit().getId(),
                        al -> al,
                        (existing, replacement) -> existing));

        boolean previousCompleted = true; // Level 1 is always unlocked

        for (LevelResponse level : levels) {
            UUID levelUuid = UUID.fromString(level.getId());

            // Calculate granular progress
            long modernCount = allModernQuizzes.stream()
                    .filter(q -> q.getLearningUnit() != null && q.getLearningUnit().getId().equals(levelUuid)).count();
            long legacyCount = allLegacyQuizzes.stream()
                    .filter(q -> q.getParent() != null && q.getParent().getId().equals(levelUuid)).count();
            int totalQuizzes = (int) (modernCount + legacyCount);

            int completedQuizzes = 0;
            // Check modern quizzes progress
            for (org.fsa_2026.company_fsa_captone_2026.entity.ContentItem mq : allModernQuizzes) {
                if (mq.getLearningUnit() != null && mq.getLearningUnit().getId().equals(levelUuid)) {
                    org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit p = progressMap.get(mq.getId());
                    if (p != null && Boolean.TRUE.equals(p.getIsCompleted())) {
                        completedQuizzes++;
                    }
                }
            }
            // Check legacy quizzes progress
            for (org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit lq : allLegacyQuizzes) {
                if (lq.getParent() != null && lq.getParent().getId().equals(levelUuid)) {
                    org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit p = progressMap.get(lq.getId());
                    if (p != null && Boolean.TRUE.equals(p.getIsCompleted())) {
                        completedQuizzes++;
                    }
                }
            }

            level.setTotalQuizzes(totalQuizzes);
            level.setCompletedQuizzes(completedQuizzes);
            level.setProgressPercentage(totalQuizzes > 0 ? (completedQuizzes * 100 / totalQuizzes) : 0);

            // Level summary progress
            org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit progress = progressMap.get(levelUuid);
            if (progress != null) {
                level.setStarsEarned(progress.getStarsEarned());
                level.setIsCompleted(progress.getIsCompleted());
            } else {
                // If level is not marked completed in DB yet, check if manually we should treat
                // it as such
                // though GameplayService handles this. For UI, we use isCompleted from DB or
                // calculate it
                level.setIsCompleted(level.getIsCompleted() != null && level.getIsCompleted());
            }

            level.setIsLocked(!previousCompleted);
            previousCompleted = level.getIsCompleted() != null && level.getIsCompleted();
        }

        return levels;
    }
}
