package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.dto.LearnerDashboardResponse;
import org.fsa_2026.company_fsa_captone_2026.service.LearnerDashboardService;
import org.fsa_2026.company_fsa_captone_2026.entity.Account;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constants.API_PREFIX + "/learner/dashboard")
@RequiredArgsConstructor
@Tag(name = "Learner Dashboard", description = "Endpoints for the new optimized Learner Dashboard UI")
public class LearnerDashboardController {

    private final LearnerDashboardService dashboardService;

    @Operation(summary = "Get aggregated dashboard data for the authenticated learner (O(1) execution)")
    @GetMapping
    public ResponseEntity<ApiResponse<LearnerDashboardResponse>> getDashboardData(@AuthenticationPrincipal Account account) {
        if (account == null) {
            return ResponseEntity.status(401).build();
        }
        LearnerDashboardResponse response = dashboardService.getDashboardData(account.getId());
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved", response));
    }
}
