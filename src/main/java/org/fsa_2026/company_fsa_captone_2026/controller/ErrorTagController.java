package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.ErrorTagResponse;
import org.fsa_2026.company_fsa_captone_2026.service.ErrorTagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Error Tag", description = "APIs for managing and retrieving Error Tags")
public class ErrorTagController {

    private final ErrorTagService errorTagService;

    @GetMapping("/public/error-tags")
    @Operation(summary = "Get all Error Tags", description = "Retrieve a list of all phoneme error tags for dropdowns")
    public ResponseEntity<ApiResponse<List<ErrorTagResponse>>> getAllErrorTags() {
        List<ErrorTagResponse> tags = errorTagService.getAllErrorTags();
        return ResponseEntity.ok(ApiResponse.success("Success", tags));
    }

    // --- Admin APIs (Assume secured by SecurityConfig) ---

    @PostMapping("/admin/error-tags")
    @Operation(summary = "Create an Error Tag", description = "Admin creates a new error tag")
    public ResponseEntity<ApiResponse<ErrorTagResponse>> createErrorTag(
            @RequestParam String tagCode,
            @RequestParam String name,
            @RequestParam(required = false) String description) {
        ErrorTagResponse res = errorTagService.createErrorTag(tagCode, name, description);
        return ResponseEntity.ok(ApiResponse.success("Created successfully", res));
    }

    @PutMapping("/admin/error-tags/{id}")
    @Operation(summary = "Update an Error Tag", description = "Admin updates an existing error tag")
    public ResponseEntity<ApiResponse<ErrorTagResponse>> updateErrorTag(
            @PathVariable UUID id,
            @RequestParam(required = false) String tagCode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description) {
        ErrorTagResponse res = errorTagService.updateErrorTag(id, tagCode, name, description);
        return ResponseEntity.ok(ApiResponse.success("Updated successfully", res));
    }

    @DeleteMapping("/admin/error-tags/{id}")
    @Operation(summary = "Delete an Error Tag", description = "Admin deletes an error tag")
    public ResponseEntity<ApiResponse<Void>> deleteErrorTag(@PathVariable UUID id) {
        errorTagService.deleteErrorTag(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted successfully", null));
    }
}
