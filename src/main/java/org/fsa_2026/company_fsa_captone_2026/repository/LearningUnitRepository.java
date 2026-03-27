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

    List<LearningUnit> findByParentId(UUID parentId);

    List<LearningUnit> findByParentIdAndType(UUID parentId, String type);

    List<LearningUnit> findByParentIdInAndType(List<UUID> parentIds, String type);

    List<LearningUnit> findByParentAndType(LearningUnit parent, String type);

    boolean existsByParentId(UUID parentId);

    Optional<LearningUnit> findByTypeAndNameIgnoreCase(String type, String name);

    void deleteByParentId(UUID parentId);
}
