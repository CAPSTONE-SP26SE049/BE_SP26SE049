package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.LessonPlan;
import org.fsa_2026.company_fsa_captone_2026.service.EducatorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/educator/lesson-plans")
@RequiredArgsConstructor
@Tag(name = "Educator Lesson Plans", description = "Curriculum templates and planning for Educators")
@PreAuthorize("hasAnyRole('EDUCATOR', 'ADMIN')")
public class EducatorLessonPlanController {

    private final EducatorService educatorService;

    @PostMapping
    @Operation(summary = "Create Lesson Plan", description = "Create a reusable set of sorted learning units")
    public ResponseEntity<ApiResponse<LessonPlan>> createPlan(
            Authentication auth,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestBody List<UUID> unitIds) {
        return ResponseEntity.ok(ApiResponse.success("Lesson Plan created", educatorService.createLessonPlan(auth.getName(), title, description, unitIds)));
    }

    @GetMapping
    @Operation(summary = "Get All Lesson Plans", description = "List all lesson plan templates created by this educator")
    public ResponseEntity<ApiResponse<List<LessonPlan>>> getPlans(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Fetched all lesson plans", educatorService.getLessonPlans(auth.getName())));
    }

    @PostMapping("/assign/{studentId}")
    @Operation(summary = "Assign Lesson Plan to Student", description = "Assign all units in a plan to a specific student")
    public ResponseEntity<ApiResponse<Void>> assignPlan(
            Authentication auth,
            @PathVariable UUID studentId,
            @RequestParam UUID planId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline) {
        educatorService.assignPlanToStudent(auth.getName(), studentId, planId, deadline);
        return ResponseEntity.ok(ApiResponse.success("Lesson Plan assigned to student", null));
    }
}
