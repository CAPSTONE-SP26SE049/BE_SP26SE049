package com.aiservice.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "classrooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Classroom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "educator_id")
    private User educator;

    private String name;

    @Column(unique = true)
    private String code;

    @OneToMany(mappedBy = "classroom", cascade = CascadeType.ALL)
    private List<ClassMember> members;
}
