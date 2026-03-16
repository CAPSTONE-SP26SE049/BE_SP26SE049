package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * PlacementRule Entity
 * Table: placement_rule
 * Cấu hình routing học sinh dựa theo kết quả bài kiểm tra đầu vào.
 *
 * Sau gộp bảng:
 * - errorTag  → LearningUnit (type = ERROR_TAG)
 * - targetDialect → LearningUnit (type = DIALECT)
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
    @JoinColumn(name = "error_tag_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_placement_rule_error_tag"))
    private LearningUnit errorTag;

    @Column(name = "threshold", nullable = false)
    private Integer threshold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dialect_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_placement_rule_dialect"))
    private LearningUnit targetDialect;

    @Column(name = "checkpoint", length = 100)
    private String checkpoint;

    @Column(name = "priority", nullable = false)
    private Integer priority;
}
