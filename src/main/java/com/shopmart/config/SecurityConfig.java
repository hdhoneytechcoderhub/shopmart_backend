package com.shopmart.config;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectMapper objectMapper;

    private static final String[] PUBLIC_PATHS = {
            "/auth/register", "/auth/login", "/auth/google", "/auth/refresh-token",
            "/auth/verify-otp", "/auth/resend-otp",
            "/auth/forgot-password", "/auth/reset-password",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/actuator/health",
            "/products/*/view"
    };

    private static final String[] PUBLIC_GET_PATHS = {
            "/products/**", "/categories/**", "/brands/**",
            "/blogs", "/blogs/*",   // published list + single post by slug (admin sub-paths stay protected)
            "/search", "/search/suggest", "/search/trending",   // history stays protected
            "/recommendations/trending",
            "/vendors/store/*",       // public storefront by slug
            "/banners",               // active banners only; /banners/all and /banners/{id} stay admin
            "/currencies", "/currencies/convert",   // currency table + conversion
            "/translations",          // localized fields lookup (admin upsert/delete stay protected)
            "/sitemap.xml", "/robots.txt", "/seo/**",  // SEO
            "/mobile/config",          // mobile app bootstrap config
            "/home",                    // public landing-page aggregation
            "/service-categories", "/services", "/services/*",  // public service catalog
            "/machines", "/machines/*",  // public machine catalog
            "/settings/public"          // public storefront settings
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                // Stateless JSON API served behind HTTPS on Railway — HSTS is safe to force.
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000))
                // No legitimate reason for this API to be framed.
                .frameOptions(frame -> frame.deny())
                // Browsers must respect the declared Content-Type; blocks MIME-sniffing attacks.
                .contentTypeOptions(contentTypeOptions -> {})
                .referrerPolicy(referrer -> referrer
                        .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .addHeaderWriter((request, response) -> {
                    // A pure JSON API doesn't execute inline scripts/styles itself; default-src 'none'
                    // with frame-ancestors 'none' blocks it being used as an XSS/clickjacking vector.
                    // Swagger UI (served from this origin when enabled) needs its own scripts/styles,
                    // so it gets a looser policy rather than breaking the docs page entirely.
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")) {
                        response.setHeader("Content-Security-Policy",
                                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'");
                    } else {
                        response.setHeader("Content-Security-Policy",
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
                    }
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
                // Public: submit a contact form (GET /contact stays admin-only via @PreAuthorize)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/contact").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/payments/webhook").permitAll()
                // Super Admin only
                .requestMatchers("/super-admin/**").hasRole("SUPER_ADMIN")
                // Actuator: /actuator/health is public above (for load balancer probes);
                // everything else (metrics, prometheus, env, etc.) is operator-only.
                .requestMatchers("/actuator/**").hasRole("SUPER_ADMIN")
                // Admin area — Admin or Super Admin
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(),
                        ApiResponse.error("Authentication required"));
            }))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
