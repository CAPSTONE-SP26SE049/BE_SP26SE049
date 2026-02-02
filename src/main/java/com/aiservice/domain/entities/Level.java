package com.aiservice.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "levels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Level {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "level_order")
    private int levelOrder;

    private String name;

    private String description;

    @Column(name = "min_stars_required")
    private int minStarsRequired;

    @OneToMany(mappedBy = "level", cascade = CascadeType.ALL)
    private List<Challenge> challenges;
}
