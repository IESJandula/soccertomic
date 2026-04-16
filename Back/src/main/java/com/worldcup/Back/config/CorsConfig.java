package com.worldcup.Back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter(
            @Value("${app.cors.allowed-origins:*}") String allowedOrigins,
            @Value("${app.cors.allow-wildcard:false}") boolean allowWildcard
    ) {
        CorsConfiguration config = new CorsConfiguration();

        // Reject wildcard origins unless explicitly enabled.
        if ("*".equals(allowedOrigins)) {
            if (!allowWildcard) {
                throw new IllegalStateException("CORS wildcard '*' is disabled. Set CORS_ALLOWED_ORIGINS to explicit domains.");
            }
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
