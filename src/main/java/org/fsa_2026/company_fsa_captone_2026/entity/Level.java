package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Level Entity (ISO Compliant)
 * Table: level (singular)
 * Learning progression levels per dialect
 */
@Entity
@Table(name = "level")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Level extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dialect_id", nullable = false)
    private Dialect dialect;

    @Column(name = "level_order", nullable = false)
    private Integer levelOrder;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_stars_required", nullable = false)
    @Builder.Default
    private Integer minStarsRequired = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_tag_id")
    private ErrorTag errorTag;

    @Column(name = "ai_threshold")
    private Integer aiThreshold;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus status = org.fsa_2026.company_fsa_captone_2026.entity.enums.ContentStatus.APPROVED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Level parent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id")
    private Level draft;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;
}
