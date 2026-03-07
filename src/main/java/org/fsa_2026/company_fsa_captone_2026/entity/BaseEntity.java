package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base Entity Class with Auditing Support (ISO Compliant)
 * ISO/IEC 9834-8: UUID Primary Keys
 * ISO 8601: TIMESTAMPTZ (Instant in Java)
 *
 * All entities inherit:
 * - id: UUID v4 primary key
 * - createdAt: ISO 8601 timestamp (UTC)
 * - updatedAt: ISO 8601 timestamp (UTC, auto-updated)
 * - createdBy: User who created the record
 * - updatedBy: User who last updated the record
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;


}
