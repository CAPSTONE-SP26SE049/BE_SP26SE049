package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningUnitRepository extends JpaRepository<LearningUnit, UUID> {

    List<LearningUnit> findByType(String type);
    List<LearningUnit> findTop1000ByType(String type);

    List<LearningUnit> findByParentId(UUID parentId);

    List<LearningUnit> findByParentIdAndType(UUID parentId, String type);

    List<LearningUnit> findByParentAndType(LearningUnit parent, String type);

    boolean existsByParentId(UUID parentId);

    Optional<LearningUnit> findByTypeAndNameIgnoreCase(String type, String name);

    /** Find the quiz linked to a specific reward (for 1-reward-per-quiz validation) */
    Optional<LearningUnit> findByRewardCatalogId(java.util.UUID rewardCatalogId);

    /** Find the quiz linked to a specific reward, excluding a given quiz (for update validation) */
    Optional<LearningUnit> findByRewardCatalogIdAndIdNot(java.util.UUID rewardCatalogId, java.util.UUID excludeId);

    /** Batch load all quizzes that have a reward linked — avoids N+1 in getAllRewards */
    List<LearningUnit> findByRewardCatalogIdIn(java.util.Collection<java.util.UUID> rewardCatalogIds);

    List<LearningUnit> findByTypeAndErrorTagIgnoreCaseAndDifficultyLevelIgnoreCase(String type, String errorTag, String difficultyLevel);
    
    List<LearningUnit> findByTypeAndErrorTagIgnoreCase(String type, String errorTag);

    long countByType(String type);
}
