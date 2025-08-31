package com.chessapp.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
                .group("v1")
                // Include serving and ingest endpoints so they appear in OpenAPI
                .packagesToScan(
                        "com.chessapp.api.web",
                        "com.chessapp.api.models.api",
                        "com.chessapp.api.serving",
                        "com.chessapp.api.ingest.api")
                .pathsToMatch("/v1/**")
                .build();
    }

    @Bean
    public OpenAPI baseOpenApi() {
        return new OpenAPI().info(
                new Info()
                        .title("ChessApp API")
                        .version("v1")
                        .description("Endpoints for ingest/train/serve/play")
        );
    }
}
