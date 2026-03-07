package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * PlacementRule Entity
 * Table: placement_rule
 * Configuration for student routing based on placement test performance
 */
@Entity
@Table(name = "placement_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_tag_id", nullable = false)
    private ErrorTag errorTag;

    @Column(name = "threshold", nullable = false)
    private Integer threshold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dialect_id", nullable = false)
    private Dialect targetDialect;

    @Column(name = "checkpoint", length = 100)
    private String checkpoint;

    @Column(name = "priority", nullable = false)
    private Integer priority;
}
