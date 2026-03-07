package org.fsa_2026.company_fsa_captone_2026.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) Configuration
 * Cấu hình tài liệu API tự động - hiển thị trên /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

        @Value("${app.api.title:FSA Capstone 2026 API}")
        private String apiTitle;

        @Value("${app.api.description:API for FSA Capstone Project 2026}")
        private String apiDescription;

        @Value("${app.api.version:1.0.0}")
        private String apiVersion;

        @Value("${app.contact.name:FSA Team}")
        private String contactName;

        @Value("${app.contact.email:contact@fsa.com}")
        private String contactEmail;

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .components(new Components()
                                                .addSecuritySchemes("bearer-jwt",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("Enter JWT token (Optional in development)")))
                                .info(new Info()
                                                .title(apiTitle)
                                                .description(apiDescription
                                                                + " - Development Mode: No Authentication Required for Swagger")
                                                .version(apiVersion)
                                                .contact(new Contact()
                                                                .name(contactName)
                                                                .email(contactEmail)));
                // Removed addSecurityItem for development mode
                // .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
        }
}
