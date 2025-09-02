package com.chessapp.api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = CodexApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"codex", "test"})
class DatasetIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Value("${app.security.jwt.secret}")
    String secret;

    @Test
    void create_get_list_and_log_contains_mdc(CapturedOutput output) throws Exception {
        String auth = TestAuth.bearerToken(secret, "USER");

        var res1 = mvc.perform(post("/v1/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .content("{\"name\":\"ds1\",\"version\":\"v1\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, notNullValue()))
                .andReturn();

        String body1 = res1.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root1 = mapper.readTree(body1);
        String id1 = root1.get("id").asText();
        assertThat(res1.getResponse().getHeader("Location"))
                .endsWith("/v1/datasets/" + id1);

        mvc.perform(get("/v1/datasets/" + id1)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id1))
                .andExpect(jsonPath("$.name").value("ds1"));

        var res2 = mvc.perform(post("/v1/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .content("{\"name\":\"ds2\",\"version\":\"v1\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body2 = res2.getResponse().getContentAsString();
        String id2 = mapper.readTree(body2).get("id").asText();

        mvc.perform(get("/v1/datasets")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id2))
                .andExpect(jsonPath("$.content[1].id").value(id1));

        boolean found = Arrays.stream(output.getOut().split("\n"))
                .anyMatch(l -> l.contains("\"dataset_id\":\"" + id1 + "\"")
                        && l.contains("\"component\":\"api\""));
        assertThat(found).isTrue();
    }

    @Test
    void missing_auth_returns401() throws Exception {
        mvc.perform(post("/v1/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ds\",\"version\":\"v1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blank_name_returns400() throws Exception {
        mvc.perform(post("/v1/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, TestAuth.bearerToken(secret, "USER"))
                        .content("{\"name\":\"\",\"version\":\"v1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformed_json_returns400() throws Exception {
        mvc.perform(post("/v1/datasets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, TestAuth.bearerToken(secret, "USER"))
                        .content("{\"name\":\"oops"))
                .andExpect(status().isBadRequest());
    }
}

