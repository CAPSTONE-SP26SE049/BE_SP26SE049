package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Lesson Plan Entity
 * Stores custom lesson plans created by educators.
 */
@Entity
@Table(name = "lesson_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonPlan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educator_id", nullable = false)
    private Account educator;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String objective;

    @Column(name = "target_students_json", columnDefinition = "TEXT")
    private String targetStudentsJson;

    @Column(name = "achievement_goals_json", columnDefinition = "TEXT")
    private String achievementGoalsJson;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PUBLISHED";
}
