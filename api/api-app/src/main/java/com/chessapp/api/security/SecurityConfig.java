package com.chessapp.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class SecurityConfig {

  @Value("${cors.allowed-origin}")
  private String allowedOrigin;

  private final MonitoringTokenAuthFilter monitoringTokenAuthFilter;
  private final RateLimitingFilter rateLimitingFilter;

  public SecurityConfig(MonitoringTokenAuthFilter m, RateLimitingFilter r) {
    this.monitoringTokenAuthFilter = m;
    this.rateLimitingFilter = r;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        // Swagger/OpenAPI & root
        .requestMatchers("/", "/error", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
        // *** M1-Smoke kompatibel: v1-APIs bleiben offen (nur Actuator geschützt) ***
        .requestMatchers("/v1/**").permitAll()
        // Actuator: health offen, Rest ADMIN (Prometheus via Sonderfilter)
        .requestMatchers("/actuator/health/**").permitAll()
        .requestMatchers("/actuator/**").hasRole("ADMIN")
        .anyRequest().authenticated()
      )
      // Custom Filter: zuerst Prometheus-Token prüfen, dann Rate-Limit anwenden
      .addFilterBefore(monitoringTokenAuthFilter, org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
      .addFilterAfter(rateLimitingFilter, MonitoringTokenAuthFilter.class)
      .cors(cors -> {});
    return http.build();
  }

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(allowedOrigin));
    cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With"));
    cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return new CorsFilter(source);
  }
}
