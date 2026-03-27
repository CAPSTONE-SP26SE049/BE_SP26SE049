package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.response.timeline.TimelineResponse;
import org.fsa_2026.company_fsa_captone_2026.service.TimelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(Constants.API_PREFIX + "/accounts")
@Tag(name = "Learning Timeline", description = "Learning timeline APIs")
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping("/{accountId}/learning-timeline")
    @Operation(
            summary = "Get learning timeline by account",
            description = "Build past, present, future timeline for account learning journey",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<TimelineResponse>> getLearningTimeline(@PathVariable UUID accountId) {
        TimelineResponse response = timelineService.getLearningTimeline(accountId);
        return ResponseEntity.ok(ApiResponse.success("Lay learning timeline thanh cong", response));
    }
}
