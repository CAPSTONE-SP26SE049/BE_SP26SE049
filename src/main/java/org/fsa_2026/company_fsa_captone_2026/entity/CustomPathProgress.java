package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Custom Path Progress Entity
 * Tracks student progress specifically within a custom learning path.
 * This progress is separate from the default system-wide progress.
 */
@Entity
@Table(name = "custom_path_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "custom_path_id", "learning_unit_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomPathProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_path_id", nullable = false)
    private CustomLearningPath customPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_unit_id", nullable = false)
    private LearningUnit learningUnit; // This could be a Quiz inside a Level

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;
}
