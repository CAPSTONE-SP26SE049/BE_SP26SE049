package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Custom Path Level Entity
 * Join table between CustomLearningPath and LearningUnit (Level/Chapter)
 */
@Entity
@Table(name = "custom_path_levels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomPathLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_path_id", nullable = false)
    private CustomLearningPath customPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id", nullable = false)
    private LearningUnit level;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
