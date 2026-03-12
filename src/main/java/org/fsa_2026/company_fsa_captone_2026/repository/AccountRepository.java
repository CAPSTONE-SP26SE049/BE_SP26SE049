package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Account Repository
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /**
     * Lấy danh sách Account theo roleCode và trạng thái active.
     * Filter tại DB thay vì load-all vào memory.
     */
    @Query("SELECT a FROM Account a WHERE a.roleCode IN :roleCodes AND a.isActive = true")
    List<Account> findAllActiveByRoleCodeIn(List<RoleCode> roleCodes);
}
