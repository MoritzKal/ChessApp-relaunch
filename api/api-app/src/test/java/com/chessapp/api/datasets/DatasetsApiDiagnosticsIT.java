package com.chessapp.api.datasets;

import com.chessapp.api.TestS3Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.test.context.ActiveProfiles;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser; // kannst du entfernen, wenn überall jwt() nutzt
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.JsonNode;


/**
 * Diagnostic, self-explaining Integration Test for /v1/datasets.
 * Fails with rich messages that tell you exactly what broke.
 */
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestS3Config.class)
class DatasetsApiDiagnosticsIT {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper om;
  static UUID datasetId; static String createdAtStr;
  static ListAppender<ILoggingEvent> appender; static Logger rootLogger;
  @BeforeAll static void attachLogCaptor(){ var ctx=(LoggerContext)LoggerFactory.getILoggerFactory();
    rootLogger=ctx.getLogger("ROOT"); appender=new ListAppender<>(); appender.start(); rootLogger.addAppender(appender);} 
  @AfterAll static void detachLogCaptor(){ if(rootLogger!=null&&appender!=null) rootLogger.detachAppender(appender); }

private RequestPostProcessor withJwtUser() {
  return jwt()
    .jwt(claims -> {
      claims.subject("sub-123");
      claims.claim("preferred_username", "test-user");
      // optional – deine Converter lesen "roles" zusätzlich
      claims.claim("roles", java.util.List.of("USER"));
      claims.claim("scope", "api"); // wird von JwtGrantedAuthoritiesConverter zu SCOPE_api
    })
    .authorities(new SimpleGrantedAuthority("ROLE_USER"));
}


  @Test @Order(1) @WithMockUser(username="test-user",roles={"USER"})
void create_shouldReturn201_withLocation_andBodyShape(CapturedOutput output) throws Exception {
  var payload = new LinkedHashMap<String,Object>();
  payload.put("name","mini-ds"); payload.put("version","v1");
  payload.put("filter", Map.of("keep", List.of("rated")));
  payload.put("split", Map.of("train",0.8,"val",0.2));
  String body = om.writeValueAsString(payload);

  // 1) NICHT sofort assertEquals(201) – erst Response einsammeln:
  MvcResult res = mvc.perform(post("/v1/datasets")
      .with(withJwtUser())               // ← hier!
      .contentType(MediaType.APPLICATION_JSON)
      .content(body))
    .andReturn();

  int sc = res.getResponse().getStatus();
  String resp = res.getResponse().getContentAsString();
  if (sc != 201) {
    System.err.println("\n=== CREATE FAILED ===");
    System.err.println("Status: " + sc);
    System.err.println("Headers: " + res.getResponse().getHeaderNames());
    System.err.println("Body:\n" + resp);
    Assertions.fail("Expected 201 Created, got " + sc + " (siehe Body/Headers oben)");
  }
    String location = res.getResponse().getHeader("Location");
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(location).withFailMessage("Missing/empty Location. Headers=%s\nBody=%s",
        headersPretty(res), resp).isNotBlank();
    JsonNode json = om.readTree(resp);
    softly.assertThat(json.hasNonNull("id")).withFailMessage("Missing 'id'. Body:\n%s", pretty(resp)).isTrue();
    if(json.hasNonNull("id")){
      softly.assertThat(Pattern.compile("^[0-9a-fA-F-]{36}$").matcher(json.get("id").asText()).matches())
        .withFailMessage("'id' not UUID. id=%s\nBody:\n%s", json.get("id").asText(), pretty(resp)).isTrue();
      datasetId = UUID.fromString(json.get("id").asText());
    }
       boolean seenInMdc = appender != null && appender.list.stream().anyMatch(ev ->
      datasetId.toString().equals(ev.getMDCPropertyMap().get("dataset_id")) &&
      "api".equals(ev.getMDCPropertyMap().get("component"))
  );

  String out = output.getOut() + output.getErr();
  boolean seenInText =
      out.contains("\"dataset_id\":\"" + datasetId + "\"") || // JSON
      out.contains("dataset_id=" + datasetId);                 // falls KeyValue-Format

  org.assertj.core.api.Assertions.assertThat(seenInMdc || seenInText)
    .withFailMessage("""
      Erwartete Log-Sichtbarkeit für dataset_id=%s nicht gefunden.
      - MDC gesehen? %s
      - In Console/JSON gesehen? %s
      --- Auszug (erste 2k Zeichen) ---
      %s
      """, datasetId, seenInMdc, seenInText,
         (out.length()>2000 ? out.substring(0,2000)+"..." : out)
    ).isTrue();
}
private UUID ensureDatasetExists() throws Exception {
  if (datasetId != null) return datasetId;

  var payload = new java.util.LinkedHashMap<String,Object>();
  payload.put("name","mini-ds");
  payload.put("version","v1");
  payload.put("filter", java.util.Map.of("keep", java.util.List.of("rated")));
  payload.put("split", java.util.Map.of("train",0.8,"val",0.2));

  String body = om.writeValueAsString(payload);

  MvcResult res = mvc.perform(post("/v1/datasets")
        .with(withJwtUser())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
      .andReturn();

  int sc = res.getResponse().getStatus();
  String resp = res.getResponse().getContentAsString();
  if (sc != 201) {
    System.err.println("\n=== ensureDatasetExists(): CREATE FAILED ===");
    System.err.println("Status: " + sc);
    System.err.println("Body:\n" + resp);
    org.junit.jupiter.api.Assertions.fail("ensureDatasetExists(): expected 201, got " + sc);
  }

  JsonNode json = om.readTree(resp);
  datasetId = java.util.UUID.fromString(json.get("id").asText());
  createdAtStr = json.get("createdAt").asText();
  return datasetId;
}

  @Test @Order(2)
void getById_shouldReturn200_andConsistentBody() throws Exception {
    UUID id = ensureDatasetExists();

  MvcResult res = mvc.perform(get("/v1/datasets/{id}", id)
        .with(withJwtUser()))
      .andExpect(status().isOk())
      .andReturn();

  String resp = res.getResponse().getContentAsString();
  JsonNode json = om.readTree(resp);
  org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
  softly.assertThat(json.path("id").asText(null)).isEqualTo(id.toString())
      .withFailMessage("GET id mismatch. expected=%s actual=%s\nBody:\n%s",
          id, json.path("id").asText(), pretty(resp));
  softly.assertThat(json.path("createdAt").asText(null)).isNotBlank()
      .withFailMessage("GET missing 'createdAt'. Body:\n%s", pretty(resp));
  softly.assertThat(json.path("filter").isObject())
      .withFailMessage("GET 'filter' not object. Body:\n%s", pretty(resp)).isTrue();
  softly.assertThat(json.path("split").isObject())
      .withFailMessage("GET 'split' not object. Body:\n%s", pretty(resp)).isTrue();
  softly.assertAll();
}

  @Test @Order(3)
void list_shouldBePaged_andSortedByCreatedAtDesc() throws Exception {
  UUID id = ensureDatasetExists();

  MvcResult res = mvc.perform(get("/v1/datasets?page=0&size=5&sort=createdAt,desc")
        .with(withJwtUser()))
      .andExpect(status().isOk())
      .andReturn();

  String resp = res.getResponse().getContentAsString();
  JsonNode json = om.readTree(resp);
  org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();

  softly.assertThat(json.has("content"))
      .withFailMessage("List missing 'content'. Body:\n%s", pretty(resp)).isTrue();

  if (json.has("content")) {
    JsonNode content = json.get("content");
    softly.assertThat(content.isArray())
        .withFailMessage("'content' should be array. Body:\n%s", pretty(resp)).isTrue();
    boolean contains = false; java.time.Instant prev = null;
    for (JsonNode item : content) {
      if (id.toString().equals(item.path("id").asText())) contains = true;
      if (item.hasNonNull("createdAt")) {
        java.time.Instant cur = java.time.Instant.parse(item.get("createdAt").asText());
        if (prev != null) {
          softly.assertThat(!cur.isAfter(prev))
              .withFailMessage("Not sorted desc: %s after %s.\nBody:\n%s", cur, prev, pretty(resp)).isTrue();
        }
        prev = cur;
      }
    }
    softly.assertThat(contains)
        .withFailMessage("List doesn't contain created id=%s.\nBody:\n%s", id, pretty(resp)).isTrue();
  }
  softly.assertAll();
}

  @Test @Order(4)
  void openapi_shouldExposeDatasetEndpoints() throws Exception {
    MvcResult res = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
    String resp = res.getResponse().getContentAsString();
    JsonNode api = om.readTree(resp); JsonNode paths = api.get("paths");
    List<String> datasetPaths = new ArrayList<>(); paths.fieldNames().forEachRemaining(p->{ if(p.startsWith("/v1/datasets")) datasetPaths.add(p);});
    // We have two dataset paths (collection + item) but three operations (GET/POST + GET)
    // Ensure at least both paths are present.
    assertThat(datasetPaths).withFailMessage("OpenAPI missing dataset endpoints. Need paths: collection & item. Actual: %s\nAll paths: %d",
      datasetPaths, paths.size()).hasSizeGreaterThanOrEqualTo(2);
  }

  @Test @Order(5)
  void unauthenticated_shouldBeRejected() throws Exception {
    int code = mvc.perform(get("/v1/datasets")).andReturn().getResponse().getStatus();
    assertThat(code==401 || code==403).withFailMessage("Expected 401/403 for missing auth, got %s", code).isTrue();
  }

  // helpers …
  private static void assumeCreated(){ assertThat(datasetId).withFailMessage("No dataset id – create() failed?").isNotNull();
    assertThat(createdAtStr).withFailMessage("No createdAt – create() failed?").isNotNull();}
  private static String pretty(String json){ try{ var om=new ObjectMapper(); return om.writerWithDefaultPrettyPrinter().writeValueAsString(om.readTree(json)); }catch(Exception e){ return json; } }
  private static String headersPretty(MvcResult res){ var names=res.getResponse().getHeaderNames(); var sb=new StringBuilder("{"); boolean first=true;
    for(String n:names){ if(!first) sb.append(", "); first=false; sb.append(n).append("=").append(res.getResponse().getHeaders(n)); } sb.append("}"); return sb.toString();}
}
