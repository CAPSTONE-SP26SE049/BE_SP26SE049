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
    private final org.fsa_2026.company_fsa_captone_2026.repository.AccountLearningUnitRepository accountLearningUnitRepository;

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

    @Transactional(readOnly = true)
    public List<LevelResponse> getLevelsWithProgress(String dialectId, org.fsa_2026.company_fsa_captone_2026.entity.Account account) {
        List<LevelResponse> levels = getLevelsByDialect(dialectId);
        
        java.util.List<org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit> progressList = 
                accountLearningUnitRepository.findByAccountId(account.getId());
                
        java.util.Map<UUID, org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit> progressMap = progressList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        al -> al.getLearningUnit().getId(),
                        al -> al,
                        (existing, replacement) -> existing // Guard against duplicates
                ));

        boolean previousCompleted = true; // Level 1 is always unlocked
        
        for (LevelResponse level : levels) {
            org.fsa_2026.company_fsa_captone_2026.entity.AccountLearningUnit progress = 
                    progressMap.get(UUID.fromString(level.getId()));
                    
            if (progress != null) {
                level.setStarsEarned(progress.getStarsEarned());
                level.setIsCompleted(progress.getIsCompleted());
            }
            
            // Logic mở khóa: 
            // Level 1 luôn mở (do previousCompleted = true)
            // Level n mở nếu Level n-1 đã hoàn thành (isCompleted = true)
            level.setIsLocked(!previousCompleted);
            
            // Cập nhật cho level tiếp theo
            previousCompleted = level.getIsCompleted() != null && level.getIsCompleted();
        }
        
        return levels;
    }
}
