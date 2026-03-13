package org.fsa_2026.company_fsa_captone_2026.repository;

import org.fsa_2026.company_fsa_captone_2026.entity.ClassroomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, UUID> {
    List<ClassroomMember> findByStudentId(UUID studentId);

    List<ClassroomMember> findByClassroomId(UUID classroomId);

    long countByClassroomId(UUID classroomId);

    boolean existsByClassroomIdAndStudentId(UUID classroomId, UUID studentId);

    void deleteByClassroomIdAndStudentId(UUID classroomId, UUID studentId);
}
