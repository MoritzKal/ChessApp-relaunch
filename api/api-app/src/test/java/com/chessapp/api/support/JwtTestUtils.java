package com.chessapp.api.support;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;

public final class JwtTestUtils {
    private JwtTestUtils() {}

    public static String signHmac256(String secret, Map<String, Object> claims, Duration ttl) {
        Algorithm alg = Algorithm.HMAC256(secret);
        long now = Instant.now().getEpochSecond();
        JWTCreator.Builder builder = JWT.create()
                .withIssuedAt(new Date(now * 1000))
                .withExpiresAt(new Date((now + ttl.getSeconds()) * 1000));
        claims.forEach((k, v) -> {
            if (v instanceof String s) {
                builder.withClaim(k, s);
            } else if (v instanceof Integer i) {
                builder.withClaim(k, i);
            } else if (v instanceof Long l) {
                builder.withClaim(k, l);
            } else if (v instanceof Boolean b) {
                builder.withClaim(k, b);
            } else if (v instanceof List<?> list) {
                builder.withClaim(k, list);
            } else {
                builder.withClaim(k, v.toString());
            }
        });
        return builder.sign(alg);
    }
}
