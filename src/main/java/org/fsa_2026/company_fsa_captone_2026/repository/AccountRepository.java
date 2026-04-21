package org.fsa_2026.company_fsa_captone_2026.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Account Repository
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /**
     * Lấy danh sách Account theo roleCode.
     * Filter tại DB thay vì load-all vào memory.
     *
     * @param roleCodes danh sách role cần lọc
     * @return danh sách account thuộc các role tương ứng, bao gồm cả active và
     *         inactive
     */
    @Query("SELECT a FROM Account a WHERE a.roleCode IN :roleCodes")
    List<Account> findAllByRoleCodeIn(
            @org.springframework.data.repository.query.Param("roleCodes") List<RoleCode> roleCodes);

    List<Account> findTop10ByIsActiveTrueOrderByTotalExperienceDesc();

    List<Account> findTop10ByRegionIgnoreCaseAndIsActiveTrueOrderByTotalExperienceDesc(String region);

    // ─── Leaderboard: Top 50 queries ─────────────────────────────────

    /**
     * Top 50 active accounts globally, ordered by totalExperience DESC.
     */
    List<Account> findTop50ByIsActiveTrueOrderByTotalExperienceDesc();

    /**
     * Top 50 active accounts globally, ordered by totalStars DESC, badgeCount DESC, streak DESC.
     */
    List<Account> findTop50ByIsActiveTrueOrderByTotalStarsDescBadgeCountDescCurrentStreakDaysDesc();

    /**
     * Top 50 active accounts for a region, ordered by totalExperience DESC.
     */
    List<Account> findTop50ByRegionIgnoreCaseAndIsActiveTrueOrderByTotalExperienceDesc(String region);

    /**
     * Top 50 active accounts for a region, ordered by totalStars DESC, badgeCount DESC, streak DESC.
     */
    List<Account> findTop50ByRegionIgnoreCaseAndIsActiveTrueOrderByTotalStarsDescBadgeCountDescCurrentStreakDaysDesc(String region);

    /**
     * Search active accounts by fullName or email containing a keyword
     * (case-insensitive).
     * Excludes the requesting user. Limit 20 results at the DB level.
     * Replaces the previous findAll() + in-memory filter anti-pattern.
     */
    @Query("SELECT a FROM Account a WHERE a.isActive = true AND a.id <> :excludeId " +
            "AND (LOWER(a.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(a.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY a.fullName ASC")
    @org.springframework.data.jpa.repository.QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.query.timeout", value = "5000"))
    List<Account> searchActiveAccountsByNameOrEmail(
            @org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("excludeId") UUID excludeId,
            org.springframework.data.domain.Pageable pageable);
}
