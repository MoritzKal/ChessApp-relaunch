package com.chessapp.api.codex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(scanBasePackages = "com.chessapp.api")
@ComponentScan(
    basePackages = "com.chessapp.api",
    excludeFilters = {
        // schließe die alte Klasse per REGEX aus (kein Compile-Time-Typ nötig)
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.chessapp\\.api\\.service\\.IngestService")
    }
)
public class CodexApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodexApplication.class, args);
    }
}