package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Learning Path Entity
 * Represents a personalized learning journey created by an educator for a
 * student.
 */
@Entity
@Table(name = "custom_learning_paths")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomLearningPath extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Account student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educator_id") // Made nullable for AI-generated paths
    private Account educator;

    @Column(name = "is_ai_generated", nullable = false)
    @Builder.Default
    private Boolean isAiGenerated = false;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "target_level", length = 50)
    private String targetLevel;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "customPath", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<CustomPathLevel> levels = new ArrayList<>();

    public void addLevel(LearningUnit level, int orderIndex) {
        CustomPathLevel pathLevel = CustomPathLevel.builder()
                .customPath(this)
                .level(level)
                .orderIndex(orderIndex)
                .build();
        levels.add(pathLevel);
    }
}
