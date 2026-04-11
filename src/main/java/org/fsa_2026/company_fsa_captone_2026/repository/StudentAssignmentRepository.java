package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.StudentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentAssignmentRepository extends JpaRepository<StudentAssignment, UUID> {
    List<StudentAssignment> findByStudentIdOrderByAssignedAtDesc(UUID studentId);
    List<StudentAssignment> findByStudentIdAndStatus(UUID studentId, String status);
    List<StudentAssignment> findByEducatorId(UUID educatorId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT sa.student.id) FROM StudentAssignment sa")
    long countDistinctStudent();
}
