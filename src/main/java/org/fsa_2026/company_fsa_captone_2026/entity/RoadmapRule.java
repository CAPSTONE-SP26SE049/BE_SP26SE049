package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Roadmap Rule Entity
 * Stores the rules for assigning difficulty levels based on score percentages.
 */
@Entity
@Table(name = "roadmap_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapRule extends BaseEntity {

    @Column(name = "min_percent", nullable = false)
    private Double minPercent;

    @Column(name = "max_percent", nullable = false)
    private Double maxPercent;

    /**
     * Comma-separated difficulty levels (e.g., "BEGINNER,INTERMEDIATE,ADVANCED")
     */
    @Column(name = "difficulties", nullable = false)
    private String difficulties;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
