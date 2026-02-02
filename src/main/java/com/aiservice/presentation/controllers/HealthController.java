package com.aiservice.presentation.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "AI Backend Service");
        return response;
    }

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to AI Backend Service");
        response.put("version", "1.0.0");
        response.put("docs", "/swagger-ui.html");
        return response;
    }
}
