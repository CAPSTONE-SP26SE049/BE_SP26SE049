package org.fsa_2026.company_fsa_captone_2026.config;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.common.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

/**
 * Security Configuration
 * Cấu hình xác thực (Authentication) và phân quyền (Authorization)
 * Sử dụng JWT Token cho Stateless API
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    // Removed WebSecurityCustomizer to avoid conflicts
    // Using permitAll() in SecurityFilterChain instead

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean

    public SecurityFilterChain filterChain(HttpSecurity http) {
        try {
            http
                    .cors(cors -> cors.configurationSource(corsConfigurationSource))
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                            .accessDeniedHandler(new JwtAccessDeniedHandler()))
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authz -> authz
                            // DEVELOPMENT MODE: Allow all access to Swagger and API docs
                            .requestMatchers(
                                    // Swagger UI endpoints
                                    "/swagger-ui.html",
                                    "/swagger-ui/**",
                                    "/v3/api-docs/**",
                                    "/v3/api-docs",
                                    "/api-docs/**",
                                    "/swagger-resources/**",
                                    "/webjars/**",

                                    // Public API endpoints
                                    "/api/v1/auth/**",
                                    "/api/v1/public/**",
                                    "/api/v1/health/**",

                                    // Development endpoints
                                    "/actuator/**",
                                    "/h2-console/**",
                                    "/error",

                                    // Root and favicon
                                    "/",
                                    "/favicon.ico")
                            .permitAll()

                            // Admin endpoints
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                            // Educator endpoints
                            .requestMatchers("/api/v1/educator/**").hasRole("EDUCATOR")

                            // Learner endpoints
                            .requestMatchers("/api/v1/learner/**").hasRole("USER")

                            // All other requests require authentication
                            .anyRequest().authenticated())
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

            // Tắt frame options cho h2-console (chỉ dùng khi phát triển)
            http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

            return http.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure security filter chain", e);
        }
    }
}
