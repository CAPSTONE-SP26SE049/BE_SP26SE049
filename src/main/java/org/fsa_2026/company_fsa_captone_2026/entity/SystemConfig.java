package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity for Dynamic System Configurations (AI keys, Prompts, etc.)
 */
@Entity
@Table(name = "system_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig extends BaseEntity {

    @Column(name = "config_key", unique = true, nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "description", length = 255)
    private String description;
}
