package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ClassroomMemberResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ClassroomResponse;
import org.fsa_2026.company_fsa_captone_2026.service.ClassroomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/classrooms")
@RequiredArgsConstructor
@Tag(name = "Classroom", description = "Classroom Management APIs")
public class ClassroomController {

    private final ClassroomService classroomService;

    @GetMapping
    @Operation(summary = "Get My Classrooms", description = "List classrooms the current user is a member of", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> getMyClassrooms(Authentication authentication) {
        log.info("Fetching classrooms for user: {}", authentication.getName());
        List<ClassroomResponse> classrooms = classroomService.getMyClassrooms(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lớp học thành công", classrooms));
    }

    @GetMapping("/{classroomId}/members")
    @Operation(summary = "Get Classroom Members", description = "List all members of a specific classroom", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<ClassroomMemberResponse>>> getClassroomMembers(
            @PathVariable String classroomId,
            Authentication authentication) {
        log.info("Fetching members for classroom {} by user: {}", classroomId, authentication.getName());
        List<ClassroomMemberResponse> members = classroomService.getClassroomMembers(authentication.getName(),
                classroomId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành viên lớp học thành công", members));
    }
}
