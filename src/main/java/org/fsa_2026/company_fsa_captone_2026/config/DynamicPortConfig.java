package org.fsa_2026.company_fsa_captone_2026.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.ServerSocket;

/**
 * Dynamic Port Configuration
 * Automatically finds an available port if default port 8082 is in use
 * Compatible with Spring Boot 4.0.2
 */
@Configuration
@Slf4j
public class DynamicPortConfig {

    /**
     * Find an available port starting from the desired port
     */
    private static int findAvailablePort(int preferredPort) {
        for (int port = preferredPort; port <= preferredPort + 100; port++) {
            try {
                new ServerSocket(port).close();
                // Port is available
                return port;
            } catch (Exception e) {
                // Port is in use, try next
                log.debug("Port {} is in use, trying next", port);
            }
        }
        return 8080; // Fallback port
    }

    /**
     * Customize embedded web server to use available port
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer(Environment env) {
        return factory -> {
            // Get configured port from application.yaml
            String portString = env.getProperty("server.port", "8082");
            int preferredPort = "0".equals(portString) ? 8082 : Integer.parseInt(portString);

            // Find available port
            int availablePort = findAvailablePort(preferredPort);

            // Set the available port using the correct API for Spring Boot 4.0+
            factory.setPort(availablePort);

            // Log the port being used
            String message = "═══════════════════════════════════════════════════════════════════\n" +
                    "🚀 Server starting on port: " + availablePort + "\n";

            if (availablePort != preferredPort) {
                message += "   (Preferred port " + preferredPort + " was in use)\n";
            }
            message += "═══════════════════════════════════════════════════════════════════";

            System.out.println(message);
            log.info("Server will listen on port: {}", availablePort);
        };
    }
}

