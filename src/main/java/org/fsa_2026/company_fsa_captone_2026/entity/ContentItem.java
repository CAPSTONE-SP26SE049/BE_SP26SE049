package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ContentItem Entity - Super Entity for Quiz and Pronunciation Practice (Challenge)
 */
@Entity
@Table(name = "content_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_unit_id", nullable = false)
    private LearningUnit learningUnit;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // PRONUNCIATION, QUIZ

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Metadata JSON for item details.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    /**
     * Items JSON (e.g., list of questions for a Quiz).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_json", columnDefinition = "jsonb")
    private String itemsJson;
}
