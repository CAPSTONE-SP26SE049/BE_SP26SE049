package org.fsa_2026.company_fsa_captone_2026.service;

import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.BaseEntity;
import org.fsa_2026.company_fsa_captone_2026.exception.ResourceNotFoundException;
import org.fsa_2026.company_fsa_captone_2026.repository.BaseRepository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Base Service Class
 * Cung cấp các CRUD methods cơ bản cho tất cả service
 *
 * @param <T> Entity type
 * @param <ID> Primary key type
 */
@Slf4j
public abstract class BaseService<T extends BaseEntity, ID extends Serializable> {

    /**
     * Subclass phải provide repository bean
     */
    protected abstract BaseRepository<T, ID> getRepository();

    /**
     * Lấy entity name cho error messages
     */
    protected abstract String getEntityName();

    /**
     * Tạo mới entity
     */
    public T create(T entity) {
        log.debug("Creating new {} entity", getEntityName());
        return getRepository().save(entity);
    }

    /**
     * Cập nhật entity
     */
    public T update(T entity) {
        log.debug("Updating {} entity with id: {}", getEntityName(), entity.getId());
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Entity ID cannot be null for update");
        }
        return getRepository().save(entity);
    }

    /**
     * Lấy entity theo ID
     */
    public T getById(ID id) {
        log.debug("Fetching {} with id: {}", getEntityName(), id);
        return getRepository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        getEntityName(),
                        "id",
                        id
                ));
    }

    /**
     * Lấy entity theo ID, return Optional
     */
    public Optional<T> findById(ID id) {
        return getRepository().findById(id);
    }

    /**
     * Lấy tất cả entities
     */
    public List<T> getAll() {
        log.debug("Fetching all {} entities", getEntityName());
        return getRepository().findAll();
    }

    /**
     * Xóa entity theo ID
     */
    public void deleteById(ID id) {
        log.debug("Deleting {} with id: {}", getEntityName(), id);
        if (!getRepository().existsById(id)) {
            throw new ResourceNotFoundException(
                    getEntityName(),
                    "id",
                    id
            );
        }
        getRepository().deleteById(id);
    }

    /**
     * Xóa entity
     */
    public void delete(T entity) {
        log.debug("Deleting {} entity", getEntityName());
        getRepository().delete(entity);
    }

    /**
     * Kiểm tra entity có tồn tại không
     */
    public boolean existsById(ID id) {
        return getRepository().existsById(id);
    }

    /**
     * Đếm tổng số entities
     */
    public long count() {
        return getRepository().count();
    }
}

