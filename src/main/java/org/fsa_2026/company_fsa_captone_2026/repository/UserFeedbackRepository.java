package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.UserFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserFeedbackRepository extends JpaRepository<UserFeedback, UUID> {

    List<UserFeedback> findBySenderIdOrderByCreatedAtDesc(UUID senderId);

    @Query("SELECT f FROM UserFeedback f WHERE " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:category IS NULL OR f.category = :category) " +
           "ORDER BY f.createdAt DESC")
    Page<UserFeedback> findAllWithFilters(
            @Param("status") String status,
            @Param("category") String category,
            Pageable pageable);
}
