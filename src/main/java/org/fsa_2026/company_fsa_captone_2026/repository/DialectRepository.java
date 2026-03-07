package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.Dialect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DialectRepository extends JpaRepository<Dialect, UUID> {
    Optional<Dialect> findByNameIgnoreCase(String name);
}
