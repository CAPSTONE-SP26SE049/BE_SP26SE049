package org.fsa_2026.company_fsa_captone_2026;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application Entry Point
 * FSA Capstone 2026 Spring Boot Application
 *
 * Configured with:
 * - Spring Security with JWT authentication
 * - CORS support for cross-origin requests
 * - JPA Auditing for automatic timestamp/user tracking
 * - Swagger/OpenAPI documentation
 * - PostgreSQL database
 * - Async email sending
 * - Scheduled tasks (Leaderboard refresh)
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

