package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Dialect Entity - Regional Accents/Dialects
 * Represents pronunciation variants by region
 */
@Entity
@Table(name = "dialect")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dialect extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
