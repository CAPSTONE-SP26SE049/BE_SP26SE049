package org.fsa_2026.company_fsa_captone_2026.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.fsa_2026.company_fsa_captone_2026.common.ErrorCode;
import org.fsa_2026.company_fsa_captone_2026.dto.AssignmentCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.AssignmentDTO;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorAssignmentDTO;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.entity.Assignment;
import org.fsa_2026.company_fsa_captone_2026.entity.LearningUnit;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.RoleCode;
import org.fsa_2026.company_fsa_captone_2026.exception.ApiException;
import org.fsa_2026.company_fsa_captone_2026.repository.AccountRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.AssignmentRepository;
import org.fsa_2026.company_fsa_captone_2026.repository.LearningUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private static final String LEARNING_UNIT_TYPE_LEVEL = "LEVEL";

    private final AssignmentRepository assignmentRepository;
    private final LearningUnitRepository learningUnitRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public AssignmentDTO createAssignment(String educatorEmail, AssignmentCreateRequest request) {
        LearningUnit learningUnit = learningUnitRepository.findById(request.getLearningUnitId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Learning unit not found"));

        if (!LEARNING_UNIT_TYPE_LEVEL.equalsIgnoreCase(learningUnit.getType())) {
            throw new ApiException(ErrorCode.INVALID_INPUT, "Learning unit must be type LEVEL");
        }

        Account assignedBy = accountRepository.findByEmail(educatorEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Educator account not found"));

        if (assignedBy.getRoleCode() != RoleCode.EDUCATOR) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only educator can assign level");
        }

        Assignment assignment = Assignment.builder()
            .classroomId(request.getClassroomId())
                .learningUnit(learningUnit)
                .assignedBy(assignedBy)
                .dueDate(request.getDueDate())
                .status(request.getStatus() == null || request.getStatus().isBlank() ? "OPEN" : request.getStatus())
                .description(request.getDescription())
                .build();

        Assignment savedAssignment = assignmentRepository.save(assignment);

        return AssignmentDTO.builder()
                .id(savedAssignment.getId())
                .levelName(learningUnit.getName())
                .dueDate(savedAssignment.getDueDate())
                .status(savedAssignment.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AssignmentDTO> getAssignmentsByStudent(UUID studentId) {
        if (!accountRepository.existsById(studentId)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Student not found");
        }
        return Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentDTO> getAssignmentsByClassroom(UUID classroomId) {
        return assignmentRepository.findAssignmentDtosByClassroomId(classroomId);
    }

    @Transactional(readOnly = true)
    public List<EducatorAssignmentDTO> getAssignmentsByEducator(UUID educatorId) {
        Account educator = accountRepository.findById(educatorId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Educator not found"));

        if (educator.getRoleCode() != RoleCode.EDUCATOR) {
            throw new ApiException(ErrorCode.INVALID_INPUT, "Account is not educator");
        }

        return assignmentRepository.findAssignmentsByEducatorId(educatorId);
    }

    @Transactional
    public void deleteAssignment(String educatorEmail, UUID assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Assignment not found"));

        Account educator = accountRepository.findByEmail(educatorEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Educator account not found"));

        if (educator.getRoleCode() != RoleCode.EDUCATOR) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only educator can delete assignment");
        }

        // Verify the educator owns this assignment
        if (!assignment.getAssignedBy().getId().equals(educator.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "You can only delete your own assignments");
        }

        assignmentRepository.delete(assignment);
    }
}
