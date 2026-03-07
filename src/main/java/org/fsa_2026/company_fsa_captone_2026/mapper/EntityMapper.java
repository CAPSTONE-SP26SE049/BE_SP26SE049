package org.fsa_2026.company_fsa_captone_2026.mapper;

import java.util.List;

/**
 * Generic Mapper Interface
 * Dùng để convert giữa Entity và DTO
 *
 * @param <E> Entity type
 * @param <D> DTO type
 */
public interface EntityMapper<E, D> {

    /**
     * Convert DTO to Entity
     */
    E toEntity(D dto);

    /**
     * Convert Entity to DTO
     */
    D toDto(E entity);

    /**
     * Convert list of DTOs to list of Entities
     */
    List<E> toEntities(List<D> dtos);

    /**
     * Convert list of Entities to list of DTOs
     */
    List<D> toDtos(List<E> entities);
}

