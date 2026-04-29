package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.entity.RoadmapRule;
import org.fsa_2026.company_fsa_captone_2026.service.RoadmapRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/educator/roadmap-rules")
@RequiredArgsConstructor
@Tag(name = "Roadmap Rules", description = "Management of personalized roadmap assignment rules")
public class RoadmapRuleController {

    private final RoadmapRuleService roadmapRuleService;

    @GetMapping
    @Operation(summary = "Get all roadmap rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDUCATOR')")
    public ResponseEntity<ApiResponse<List<RoadmapRule>>> getAllRules() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quy tắc lộ trình thành công", roadmapRuleService.getAllRules()));
    }

    @PostMapping
    @Operation(summary = "Save or update a roadmap rule")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDUCATOR')")
    public ResponseEntity<ApiResponse<RoadmapRule>> saveRule(@RequestBody RoadmapRule rule) {
        return ResponseEntity.ok(ApiResponse.success("Lưu quy tắc thành công", roadmapRuleService.saveRule(rule)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a roadmap rule")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDUCATOR')")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
        roadmapRuleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa quy tắc thành công", null));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset rules to default")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDUCATOR')")
    public ResponseEntity<ApiResponse<Void>> resetToDefault() {
        roadmapRuleService.resetToDefault();
        return ResponseEntity.ok(ApiResponse.success("Đã đặt lại quy tắc mặc định", null));
    }
}
