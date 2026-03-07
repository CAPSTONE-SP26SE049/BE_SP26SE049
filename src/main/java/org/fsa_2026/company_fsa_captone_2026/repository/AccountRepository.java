package org.fsa_2026.company_fsa_captone_2026.repository;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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
}
