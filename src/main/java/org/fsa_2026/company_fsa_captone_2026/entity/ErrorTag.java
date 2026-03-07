package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * ErrorTag Entity
 * Table: error_tag
 * ISO-compliant lookup table for phonetic error types
 */
@Entity
@Table(name = "error_tag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorTag extends BaseEntity {

    @Column(name = "tag_code", nullable = false, unique = true, length = 50)
    private String tagCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "errorTag", fetch = FetchType.LAZY)
    private List<PlacementRule> placementRules;
}
