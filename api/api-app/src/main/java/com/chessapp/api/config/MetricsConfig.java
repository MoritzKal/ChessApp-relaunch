package com.chessapp.api.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

  @Bean
  MeterRegistryCustomizer<MeterRegistry> commonTags(
      @Value("${app.name:chessapp}") String app,
      @Value("${app.component:api}") String component,
      @Value("${APP_ENV:local}") String env) {
    return registry -> registry.config().commonTags(
        "app", app,
        "component", component,
        "env", env
    );
  }

  // Entschärft HTTP-URI-Tag (IDs → {id})
  @Bean
  MeterFilter normalizeUriTag() {
    return MeterFilter.replaceTagValues("uri", uri -> {
      if (uri == null) return "UNKNOWN";
      return uri
          .replaceAll("/\\d+", "/{id}")
          .replaceAll("/[0-9a-fA-F-]{8,}", "/{id}");
    });
  }
}

