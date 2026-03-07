package org.fsa_2026.company_fsa_captone_2026.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application Configuration
 * Khai báo các Bean tiện ích dùng chung trong ứng dụng
 */
@Configuration
public class AppConfig {

    /**
     * ObjectMapper bean - sử dụng cho JSON processing
     * Được cấu hình để:
     * - Bỏ qua null fields
     * - Hỗ trợ Java 8+ time API
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        return objectMapper;
    }

    /**
     * RestTemplate bean - dùng để gọi external API
     * Có thể thêm interceptor, error handler tùy theo nhu cầu
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
