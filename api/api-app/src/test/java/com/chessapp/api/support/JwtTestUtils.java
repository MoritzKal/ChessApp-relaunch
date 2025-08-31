package com.chessapp.api.support;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class JwtTestUtils {

    private JwtTestUtils() {
    }

    public static final String SECRET = "test-secret";

    public static String createToken(String subject, String secret, String... roles) {
        try {
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

            StringBuilder payloadBuilder = new StringBuilder();
            payloadBuilder.append("{\"sub\":\"").append(subject).append("\"");
            payloadBuilder.append(",\"exp\":").append(Instant.now().plusSeconds(3600).getEpochSecond());
            if (roles != null && roles.length > 0) {
                payloadBuilder.append(",\"roles\":[");
                for (int i = 0; i < roles.length; i++) {
                    if (i > 0) {
                        payloadBuilder.append(',');
                    }
                    payloadBuilder.append('"').append(roles[i]).append('"');
                }
                payloadBuilder.append(']');
            }
            payloadBuilder.append('}');
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadBuilder.toString().getBytes(StandardCharsets.UTF_8));

            String unsigned = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));

            return unsigned + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
