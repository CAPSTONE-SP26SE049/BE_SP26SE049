package org.fsa_2026.company_fsa_captone_2026.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS Configuration
 * Cấu hình kết nối từ Frontend - cho phép cross-origin requests
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Cấu hình allowed origins từ properties
        List<String> origins = new ArrayList<>();
        for (String origin : allowedOrigins.split(",")) {
            origins.add(origin.trim());
        }
        configuration.setAllowedOrigins(origins);

        // Cấu hình allowed methods
        List<String> methods = new ArrayList<>();
        for (String method : allowedMethods.split(",")) {
            methods.add(method.trim());
        }
        configuration.setAllowedMethods(methods);

        // Cấu hình allowed headers
        configuration.setAllowedHeaders(List.of(allowedHeaders.split(",")));

        // Cấu hình credentials
        configuration.setAllowCredentials(allowCredentials);

        // Cấu hình max age (cache)
        configuration.setMaxAge(maxAge);

        // Exposed headers - cho phép client access những headers này
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

