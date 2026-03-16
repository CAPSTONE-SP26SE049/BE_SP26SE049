package org.fsa_2026.company_fsa_captone_2026.base.crud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseCrudRepository<E, I> extends
        JpaRepository<E, I>,
        JpaSpecificationExecutor<E> {
}
