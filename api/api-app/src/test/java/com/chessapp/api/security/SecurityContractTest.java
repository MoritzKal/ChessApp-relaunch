package com.chessapp.api.security;

import com.chessapp.api.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = {SecurityContractTest.TestApp.class, SecurityConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.jwt.secret=eine-lange-zufällige-zeichenketteeine-lange-zufällige-zeichenkette",
        "management.endpoints.web.exposure.include=health,prometheus",
        // falls vorhanden: Dev-Bypass sicher ausschalten
        "app.security.dev-token.enabled=false",
        "app.security.devToken.enabled=false"
})
class SecurityContractTest {

    @SpringBootConfiguration
    @Import(SecurityConfig.class)
    static class TestApp {
        @RestController
        static class TestController {
            @GetMapping(value = "/v1/models", produces = MediaType.APPLICATION_JSON_VALUE)
            public List<Map<String, Object>> models() {
                return List.of(Map.of("modelId", "policy_tiny", "displayName", "Policy Tiny"));
            }
            @GetMapping("/actuator/health")
            public Map<String, String> health() {
                return Map.of("status", "UP");
            }
            // /actuator/prometheus wird aus Security-Sicht geprüft; Inhalt ist hier egal
        }
    }

    @Autowired
    MockMvc mvc;

    private String token(String scope, long ttlSeconds, String secret) {
        var raw = secret.getBytes(StandardCharsets.UTF_8);
        var encoder = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(raw));
        var now = Instant.now();

        Instant expiresAt = now.plusSeconds(ttlSeconds);
        Instant issuedAt = ttlSeconds < 0 ? expiresAt.minusSeconds(1) : now;

        var claims = JwtClaimsSet.builder()
                .subject("dev-user")
                .issuer("chessapp-dev")
                .audience(List.of("api"))
                .claim("scope", scope)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    private String good(String scope) {
        return token(scope, 3600, "eine-lange-zufällige-zeichenketteeine-lange-zufällige-zeichenkette");
    }
    private String badSig(String scope) {
        return token(scope, 3600, "ffffffffffffffffffffffffffffffff");
    }
    private String expired(String scope) {
        return token(scope, -60, "0123456789abcdef0123456789abcdef");
    }

    @Test
    void health_is_public() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void models_requires_auth() throws Exception {
        mvc.perform(get("/v1/models")).andExpect(status().isUnauthorized());
    }

    @Test
    void models_with_valid_token_is_ok() throws Exception {
        mvc.perform(get("/v1/models")
                        .header("Authorization", "Bearer " + good("read")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void invalid_signature_is_unauthorized() throws Exception {
        mvc.perform(get("/v1/models")
                        .header("Authorization", "Bearer " + badSig("readabcde")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expired_token_is_unauthorized() throws Exception {
        mvc.perform(get("/v1/models")
                        .header("Authorization", "Bearer " + expired("read")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prometheus_requires_monitoring_scope() throws Exception {
        mvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + good("read")))
                .andExpect(status().isForbidden());
    }
}
