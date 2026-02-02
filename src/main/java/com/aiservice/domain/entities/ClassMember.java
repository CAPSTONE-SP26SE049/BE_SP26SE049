package com.aiservice.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
}
