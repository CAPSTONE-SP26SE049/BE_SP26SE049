package com.aiservice.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "regions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegionType name;

    private String description;

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL)
    private List<Level> levels;
}
