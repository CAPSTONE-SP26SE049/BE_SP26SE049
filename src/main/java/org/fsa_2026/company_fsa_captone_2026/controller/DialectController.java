package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.common.Constants;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.fsa_2026.company_fsa_captone_2026.dto.DialectResponse;
import org.fsa_2026.company_fsa_captone_2026.service.DialectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(Constants.API_PREFIX + "/dialects")
@RequiredArgsConstructor
@Tag(name = "Dialect", description = "Vietnamese Regional Dialect APIs")
public class DialectController {

    private final DialectService dialectService;

    @GetMapping
    @Operation(summary = "Get All Dialects", description = "List all supported Vietnamese regional dialects")
    public ResponseEntity<ApiResponse<List<DialectResponse>>> getAllDialects() {
        log.info("Get all dialects");
        List<DialectResponse> dialects = dialectService.getAllDialects();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách giọng địa phương thành công", dialects));
    }
}
