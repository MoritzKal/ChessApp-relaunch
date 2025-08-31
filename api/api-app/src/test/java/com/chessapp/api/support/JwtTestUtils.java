package com.chessapp.api.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Utility to build HS256 JWTs for tests. */
public final class JwtTestUtils {
    private static final String SECRET = "testsecret"; // sync with application-test.yml

    private JwtTestUtils() {}

    public static String token(String sub, List<String> roles) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String headerJson = mapper.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT"));
            String payloadJson = mapper.writeValueAsString(payload(sub, roles));
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = sign(header + "." + payload);
            return header + "." + payload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String token(String sub, String... roles) {
        return token(sub, roles == null ? List.of() : List.of(roles));
    }

    private static Map<String, Object> payload(String sub, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        if (roles != null && !roles.isEmpty()) {
            claims.put("roles", roles);
        }
        return claims;
    }

    private static String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
