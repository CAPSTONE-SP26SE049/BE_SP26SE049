package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.AssignmentCreateRequest;
import org.fsa_2026.company_fsa_captone_2026.dto.AssignmentDTO;
import org.fsa_2026.company_fsa_captone_2026.dto.EducatorAssignmentDTO;
import org.fsa_2026.company_fsa_captone_2026.service.AssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Assignment", description = "Assignment management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/api/v1/educator/assignments")
    @Operation(summary = "Create assignment", description = "Create assignment for a classroom and level. Only educator can assign.")
    public ResponseEntity<ApiResponse<AssignmentDTO>> createAssignment(
            Authentication authentication,
            @Valid @RequestBody AssignmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Tạo assignment thành công",
                        assignmentService.createAssignment(authentication.getName(), request)));
    }

    @GetMapping("/api/v1/educator/assignments/student/{studentId}")
    @Operation(summary = "Get assignments by student", description = "Get all assignments of classrooms where student participates")
    public ResponseEntity<ApiResponse<List<AssignmentDTO>>> getAssignmentsByStudent(@PathVariable UUID studentId) {
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách assignment theo student thành công",
                        assignmentService.getAssignmentsByStudent(studentId)));
    }

    @GetMapping("/api/v1/educator/assignments/classroom/{classroomId}")
    @Operation(summary = "Get assignments by classroom", description = "Get all assignments of one classroom")
    public ResponseEntity<ApiResponse<List<AssignmentDTO>>> getAssignmentsByClassroom(@PathVariable UUID classroomId) {
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách assignment theo classroom thành công",
                        assignmentService.getAssignmentsByClassroom(classroomId)));
    }

    @GetMapping("/api/v1/assignments/educator/{educatorId}")
    @Operation(summary = "Get assignments by educator", description = "Get all assignments created by educator with classroom name and level name")
    public ResponseEntity<ApiResponse<List<EducatorAssignmentDTO>>> getAssignmentsByEducator(@PathVariable UUID educatorId) {
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách assignment theo educator thành công",
                        assignmentService.getAssignmentsByEducator(educatorId)));
    }

    @DeleteMapping("/api/v1/educator/assignments/{assignmentId}")
    @Operation(summary = "Delete assignment", description = "Remove a chapter assignment from a class. Only the educator who created the assignment can delete it.")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(
            Authentication authentication,
            @PathVariable UUID assignmentId) {
        assignmentService.deleteAssignment(authentication.getName(), assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Gỡ học phần khỏi lớp thành công", null));
    }
}
