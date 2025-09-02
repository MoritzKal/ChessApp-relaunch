package com.chessapp.api.ingest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.awaitility.Awaitility.await; 
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import java.time.Duration;
import com.jayway.jsonpath.JsonPath;
import com.chessapp.api.testutil.TestAuth;

@SpringBootTest
@AutoConfigureMockMvc
class IngestControllerIT {
  @Autowired MockMvc mvc;

  @Test @WithMockUser(username="admin", roles={"ADMIN"})
  void start_then_poll_succeeds() throws Exception {
    var post = mvc.perform(post("/v1/ingest").with(TestAuth.jwtAdmin()))
        .andExpect(status().isAccepted())
        .andExpect(header().exists("Location"))
        .andReturn();

    var runId = JsonPath.read(post.getResponse().getContentAsString(), "$.runId");
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
      mvc.perform(get("/v1/ingest/{id}", runId).with(TestAuth.jwtAdmin()))
         .andExpect(status().isOk())
         .andExpect(jsonPath("$.status")
         .value(anyOf(is("RUNNING"), is("SUCCEEDED"), is("FAILED"))))
    );
  }

  @Test @WithMockUser(username="admin", roles={"ADMIN"})
  void legacy_alias_redirects() throws Exception {
    mvc.perform(post("/v1/data/import").with(TestAuth.jwtAdmin()))
       .andExpect(status().isPermanentRedirect())
       .andExpect(header().string("Location", "/v1/ingest"));
  }
}

