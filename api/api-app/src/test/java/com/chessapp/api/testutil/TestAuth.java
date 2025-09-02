package com.chessapp.api.testutil;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import com.chessapp.api.support.JwtTestUtils;

/** Test helpers for authenticated requests. */
public final class TestAuth {
    private TestAuth() {}

    /**
     * Create a {@link RequestPostProcessor} that injects a JWT with the given roles.
     * Scope "api" and a default username are always included.
     */
    public static RequestPostProcessor jwtWithRoles(String... roles) {
        return SecurityMockMvcRequestPostProcessors.jwt()
            .jwt(jwt -> {
                jwt.subject("sub-123");
                jwt.claim("preferred_username", "test-user");
                jwt.claim("scope", "api");
                if (roles != null && roles.length > 0) {
                    jwt.claim("roles", List.of(roles));
                }
            })
            .authorities(Arrays.stream(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList());
    }

    /** Convenience for a regular USER token. */
    public static RequestPostProcessor jwtUser() {
        return jwtWithRoles("USER");
    }

    /** Convenience for an ADMIN token. */
    public static RequestPostProcessor jwtAdmin() {
        return jwtWithRoles("ADMIN");
    }

    /** Convenience for a MONITORING token. */
    public static RequestPostProcessor jwtMonitoring() {
        return jwtWithRoles("MONITORING");
    }

    /** Create a raw JWT string signed with the given secret. */
    public static String token(String secret, String... roles) {
        return JwtTestUtils.signHmac256(secret,
                Map.of(
                        "preferred_username", "test-user",
                        "roles", List.of(roles),
                        "scope", "api"
                ),
                Duration.ofMinutes(5));
    }

    /** Convenience for an Authorization header value "Bearer ...". */
    public static String bearerToken(String secret, String... roles) {
        return "Bearer " + token(secret, roles);
    }
}
