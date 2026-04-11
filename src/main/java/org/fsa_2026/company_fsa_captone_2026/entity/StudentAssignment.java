package org.fsa_2026.company_fsa_captone_2026.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Account student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educator_id", nullable = false)
    private Account educator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_unit_id", nullable = false)
    private LearningUnit learningUnit;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED, OVERDUE

    @CreationTimestamp
    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime deadline;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
