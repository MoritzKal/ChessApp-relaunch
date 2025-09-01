package com.chessapp.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class ApiSecurityConfig {

  @Value("${cors.allowed-origin:http://localhost:5173}")
  private String allowedOrigin;

  private final MonitoringTokenAuthFilter monitoringTokenAuthFilter;
  private final RateLimitingFilter rateLimitingFilter;

  public ApiSecurityConfig(MonitoringTokenAuthFilter m, RateLimitingFilter r) {
    this.monitoringTokenAuthFilter = m;
    this.rateLimitingFilter = r;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        // Swagger/OpenAPI & Basics
        .requestMatchers("/", "/error", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
        // M1: v1-APIs offen lassen (Smoke)
        .requestMatchers("/v1/**").permitAll()
        // Actuator: health & prometheus offen (prometheus wird durch Filter geschützt)
        .requestMatchers("/actuator/health/**").permitAll()
        .requestMatchers("/actuator/prometheus").permitAll()
        // übrige Actuator-Endpunkte nur für ADMIN
        .requestMatchers("/actuator/**").hasRole("ADMIN")
        .anyRequest().authenticated()
      )
      .cors(cors -> {});
    http.addFilterBefore(monitoringTokenAuthFilter,
        org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
    http.addFilterAfter(rateLimitingFilter, MonitoringTokenAuthFilter.class);
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(allowedOrigin));
    cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With"));
    cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
