package org.fsa_2026.company_fsa_captone_2026.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Dùng để kiểm tra trạng thái ứng dụng
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Public API endpoints")
public class HealthCheckController {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${app.api.version}")
    private String apiVersion;

    /**
     * Health Check Endpoint
     * GET /api/v1/public/health
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check if application is running")
    public ApiResponse<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", appName);
        health.put("version", apiVersion);
        health.put("timestamp", System.currentTimeMillis() + "");

        return ApiResponse.success("Ứng dụng đang hoạt động", health);
    }

    /**
     * Info Endpoint - Lấy thông tin ứng dụng
     * GET /api/v1/public/info
     */
    @GetMapping("/info")
    @Operation(summary = "Application Info", description = "Get application information")
    public ApiResponse<Map<String, String>> getInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("name", appName);
        info.put("version", apiVersion);
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));

        return ApiResponse.success("Lấy thông tin ứng dụng thành công", info);
    }
}

