package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.EntryTestRegionCategory;

/**
 * Entity for Entry Test Questions.
 * Used to diagnose user's regional pronunciation errors.
 */
@Entity
@Table(name = "entry_test_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryTestQuestion extends BaseEntity {

    @Column(name = "target_text", nullable = false, columnDefinition = "TEXT")
    private String targetText;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_category", nullable = false, length = 50)
    private EntryTestRegionCategory regionCategory;
}
