package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * SessionDetail Entity - tracks individual attempts/items within a StudySession
 */
@Entity
@Table(name = "session_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StudySession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_item_id", nullable = false)
    private ContentItem contentItem;

    @Column(name = "is_passed", nullable = false)
    @Builder.Default
    private Boolean isPassed = false;

    @Column(name = "score_overall", precision = 5, scale = 2)
    private BigDecimal scoreOverall;

    @Column(name = "attempt_metadata_json", columnDefinition = "jsonb")
    private String attemptMetadataJson;
}
