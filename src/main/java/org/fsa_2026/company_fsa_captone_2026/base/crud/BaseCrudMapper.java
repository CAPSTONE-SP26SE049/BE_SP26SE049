package org.fsa_2026.company_fsa_captone_2026.base.crud;

import org.mapstruct.MappingTarget;

public interface BaseCrudMapper<E, D> {

    D toResponse(E entity);

    E toEntity(D dto);

    void update(@MappingTarget E entity, D dto);
}
