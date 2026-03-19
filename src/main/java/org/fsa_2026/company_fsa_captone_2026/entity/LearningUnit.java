package org.fsa_2026.company_fsa_captone_2026.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * LearningUnit Entity - Super Entity for Dialect and Level
 */
@Entity
@Table(name = "learning_unit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningUnit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private LearningUnit parent;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // DIALECT, LEVEL

    /**
     * Metadata JSON storing type-specific fields.
     * Dialect: {"description": ""}
     * Level: {"level_order": 1, "min_stars_required": 10, ...}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;
}
