package com.chessapp.api.codex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import com.chessapp.api.testutil.TestAuth;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = CodexApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // Avoid minimal logback-codex.xml provider error in tests
                "logging.config=classpath:logback-spring.xml"
        }
)
@ActiveProfiles("codex")
@Testcontainers
class ApiSmokeIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.baseline-on-migrate", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    DataSource dataSource;

    @Autowired
    TestRestTemplate rest;

    @LocalServerPort
    int port;

    @Value("${app.security.jwt.secret}")
    String secret;

    @Test
    void contextStarts_andDataSourceAndFlywayHealthy_andHealthIsUp() throws Exception {
        // DataSource connects
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            assertThat(c.isValid(5)).isTrue();
            // Migrations executed: expect a known table created by Flyway
            try (ResultSet rs = st.executeQuery("select count(*) from information_schema.tables where table_name = 'users'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThan(0);
            }
        }

        // Actuator health is UP
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestAuth.token(secret, "USER"));
        ResponseEntity<String> resp = rest.exchange("http://localhost:" + port + "/actuator/health", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).contains("\"status\":\"UP\"");
    }
}
