package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "lesson_plan_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_plan_id", nullable = false)
    private LessonPlan lessonPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_unit_id", nullable = false)
    private LearningUnit learningUnit;

    @Column(name = "order_index")
    private Integer orderIndex;
}
