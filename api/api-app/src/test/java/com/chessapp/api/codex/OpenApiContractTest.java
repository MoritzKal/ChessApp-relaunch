package com.chessapp.api.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest {

    @Test
    void openapi_isAvailable_whenApiRunning_locally() throws Exception {
        String url = "http://localhost:8080/v3/api-docs";
        String body = tryGet(url);
        Assumptions.assumeTrue(body != null, "API not reachable at " + url + "; skipping");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(body);
        assertThat(root.has("openapi")).isTrue();
        assertThat(root.path("paths").isObject()).isTrue();
    }

    private String tryGet(String urlStr) {
        URL url;
        try {
            try {
                url = new URI(urlStr).toURL();
            } catch (URISyntaxException e) {
                return null;
            }
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (var in = conn.getInputStream()) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}

