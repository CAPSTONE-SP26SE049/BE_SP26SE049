package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningUnitRepository extends JpaRepository<LearningUnit, UUID> {

    List<LearningUnit> findByType(String type);

    /** JOIN FETCH parent để buildQuizResponse luôn có levelId (thống kê quiz theo học phần). */
    @Query("select distinct q from LearningUnit q left join fetch q.parent where q.type = :type")
    List<LearningUnit> findByTypeWithParentFetched(@Param("type") String type);

    List<LearningUnit> findByParentId(UUID parentId);

    List<LearningUnit> findByParentIdAndType(UUID parentId, String type);

    List<LearningUnit> findByParentIdInAndType(List<UUID> parentIds, String type);

    List<LearningUnit> findByParentAndType(LearningUnit parent, String type);

    boolean existsByParentId(UUID parentId);

    Optional<LearningUnit> findByTypeAndNameIgnoreCase(String type, String name);

    void deleteByParentId(UUID parentId);

    /**
     * Đếm số bài kiểm tra (LearningUnit type QUIZ) theo học phần (parent_id).
     * Dùng cho admin thống kê trên thẻ level — không phụ thuộc lazy parent hay JSON.
     */
    @Query(value = """
            SELECT lu.parent_id AS level_id, COUNT(*)::bigint AS cnt
            FROM learning_unit lu
            WHERE UPPER(COALESCE(lu.type, '')) = 'QUIZ' AND lu.parent_id IS NOT NULL
            GROUP BY lu.parent_id
            """, nativeQuery = true)
    List<Object[]> countQuizzesGroupedByParentId();
}
