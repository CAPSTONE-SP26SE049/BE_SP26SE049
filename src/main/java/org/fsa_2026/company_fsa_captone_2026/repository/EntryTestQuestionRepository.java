package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.EntryTestQuestion;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.EntryTestRegionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EntryTestQuestionRepository extends JpaRepository<EntryTestQuestion, UUID> {

    List<EntryTestQuestion> findByRegionCategory(EntryTestRegionCategory regionCategory);

    @Query(value = "SELECT * FROM entry_test_question WHERE region_category = :category ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<EntryTestQuestion> findRandomByRegion(@Param("category") String category, @Param("limit") int limit);
}
