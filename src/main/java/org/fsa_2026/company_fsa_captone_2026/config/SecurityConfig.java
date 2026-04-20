package org.fsa_2026.company_fsa_captone_2026.config;

import lombok.RequiredArgsConstructor;
import org.fsa_2026.company_fsa_captone_2026.common.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "https://speakvn-frontend-221596280724.asia-southeast1.run.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

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
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .csrf(csrf -> csrf.disable())
                    .formLogin(form -> form.disable())
                    .httpBasic(basic -> basic.disable())
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint((request, response, authException) -> {
                                // Trả về 401 thuần REST, tránh browser popup Basic Auth (WWW-Authenticate)
                                response.setHeader("WWW-Authenticate", "");
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("text/plain;charset=UTF-8");
                                response.getWriter().write("Unauthorized");
                            })
                            .accessDeniedHandler(new JwtAccessDeniedHandler()))
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authz -> authz
                            // DEVELOPMENT MODE: Allow all access to Swagger and API docs
                            .requestMatchers(
                                    // WebSocket/SockJS endpoints must be public
                                    "/ws/**",
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

                                    // Development actuator
                                    "/actuator/**",

                                    // Root and favicon
                                    "/",
                                    "/error",
                                    "/favicon.ico",

                                    // Public badge catalog (learners can view without login)
                                    "/api/v1/public/badges/catalog",
                                    "/api/v1/learner/my-badges",

                                    // Leaderboard public endpoints (optional auth for myRank)
                                    "/api/v1/leaderboards/global",
                                    "/api/v1/leaderboards/region/**")
                            .permitAll()

                            // Admin endpoints
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                            // Educator endpoints
                            .requestMatchers("/api/v1/educator/**").hasAnyRole("EDUCATOR", "ADMIN")

                            // Learner endpoints
                            .requestMatchers("/api/v1/learner/**").hasRole("USER")

                            // All other requests require authentication
                            .anyRequest().authenticated())
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

            // Bảo vệ Clickjacking - chỉ cho phép frame từ cùng origin
            http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

            return http.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure security filter chain", e);
        }
    }
}
