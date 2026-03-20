package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.dto.AssignmentDTO;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorAssignmentDTO;
import org.fsa_2026.company_fsa_captone_2026.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByClassroomId(UUID classroomId);

    @Query("""
            SELECT new org.fsa_2026.company_fsa_captone_2026.dto.AssignmentDTO(
                a.id,
                lu.name,
                a.dueDate,
                a.status
            )
            FROM Assignment a
            JOIN a.learningUnit lu
            WHERE a.classroom.id = :classroomId
            ORDER BY a.dueDate ASC
            """)
    List<AssignmentDTO> findAssignmentDtosByClassroomId(@Param("classroomId") UUID classroomId);

    @Query("""
            SELECT new org.fsa_2026.company_fsa_captone_2026.dto.AssignmentDTO(
                a.id,
                lu.name,
                a.dueDate,
                a.status
            )
            FROM Assignment a
            JOIN a.learningUnit lu
            JOIN ClassroomMember cm ON cm.classroom.id = a.classroom.id
            WHERE cm.student.id = :studentId
            ORDER BY a.dueDate ASC
            """)
    List<AssignmentDTO> findAssignmentDtosByStudentId(@Param("studentId") UUID studentId);

    @Query("""
            SELECT new org.fsa_2026.company_fsa_captone_2026.dto.EducatorAssignmentDTO(
                a.id,
                c.name,
                lu.name,
                a.dueDate,
                a.status,
                a.createdAt
            )
            FROM Assignment a
            JOIN a.classroom c
            JOIN a.learningUnit lu
            WHERE a.assignedBy.id = :educatorId
            ORDER BY a.createdAt DESC
            """)
    List<EducatorAssignmentDTO> findAssignmentsByEducatorId(@Param("educatorId") UUID educatorId);
}
