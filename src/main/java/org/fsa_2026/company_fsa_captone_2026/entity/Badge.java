package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Badge Entity (ISO Compliant)
 * Table: badge (singular)
 * Achievement badges with JSONB criteria
 */
@Entity
@Table(name = "badge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", nullable = false, length = 500)
    private String iconUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> criteriaJson;
}

