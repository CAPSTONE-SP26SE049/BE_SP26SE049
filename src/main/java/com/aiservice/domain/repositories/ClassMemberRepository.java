package com.aiservice.domain.repositories;

import com.aiservice.domain.entities.ClassMember;
import com.aiservice.domain.entities.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {
    List<ClassMember> findByClassroom(Classroom classroom);
}

