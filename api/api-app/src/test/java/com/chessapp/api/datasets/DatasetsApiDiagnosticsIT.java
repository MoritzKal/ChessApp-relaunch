package com.chessapp.api.datasets;

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

/**
 * Diagnostic, self-explaining Integration Test for /v1/datasets.
 * Fails with rich messages that tell you exactly what broke.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatasetsApiDiagnosticsIT {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper om;
  static UUID datasetId; static String createdAtStr;
  static ListAppender<ILoggingEvent> appender; static Logger rootLogger;
  @BeforeAll static void attachLogCaptor(){ var ctx=(LoggerContext)LoggerFactory.getILoggerFactory();
    rootLogger=ctx.getLogger("ROOT"); appender=new ListAppender<>(); appender.start(); rootLogger.addAppender(appender);} 
  @AfterAll static void detachLogCaptor(){ if(rootLogger!=null&&appender!=null) rootLogger.detachAppender(appender); }

  @Test @Order(1) @WithMockUser(username="test-user",roles={"USER"})
  void create_shouldReturn201_withLocation_andBodyShape() throws Exception {
    var payload = new LinkedHashMap<String,Object>();
    payload.put("name","mini-ds"); payload.put("version","v1");
    payload.put("filter", Map.of("keep", List.of("rated")));
    payload.put("split", Map.of("train",0.8,"val",0.2));
    String body = om.writeValueAsString(payload);
    MvcResult res = mvc.perform(post("/v1/datasets")
        .contentType(MediaType.APPLICATION_JSON).content(body))
      .andExpect(status().isCreated()).andReturn();
    String resp = res.getResponse().getContentAsString();
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
    softly.assertThat(json.hasNonNull("createdAt"))
      .withFailMessage("Missing 'createdAt'. Body:\n%s", pretty(resp)).isTrue();
    if(json.hasNonNull("createdAt")){ createdAtStr=json.get("createdAt").asText();
      try{ Instant.parse(createdAtStr);}catch(Exception e){
        softly.fail("'createdAt' not ISO-8601. createdAt=%s\nError=%s\nBody:\n%s",
          createdAtStr,e.toString(),pretty(resp));}}
    softly.assertThat(json.path("name").asText(null)).isEqualTo(payload.get("name"))
      .withFailMessage("'name' not echoed. expected=%s actual=%s\nBody:\n%s",
        payload.get("name"), json.path("name").asText(), pretty(resp));

    if(location!=null && datasetId!=null){
      softly.assertThat(location.endsWith("/v1/datasets/"+datasetId))
        .withFailMessage("Location mismatch. expected suffix=/v1/datasets/%s actual=%s",
          datasetId, location).isTrue();}
    boolean hasDatasetMdc = appender.list.stream().anyMatch(ev ->
      datasetId!=null && datasetId.toString().equals(ev.getMDCPropertyMap().get("dataset_id")));
    softly.assertThat(hasDatasetMdc)
      .withFailMessage("No log with MDC dataset_id=%s. Captured=%d. Tip: set MDC in create-path and keep JSON logging.", datasetId, appender.list.size()).isTrue();
    softly.assertAll();
  }

  @Test @Order(2) @WithMockUser(username="test-user",roles={"USER"})
  void getById_shouldReturn200_andConsistentBody() throws Exception {
    assumeCreated();
    MvcResult res = mvc.perform(get("/v1/datasets/{id}", datasetId))
      .andExpect(status().isOk()).andReturn();
    String resp = res.getResponse().getContentAsString();
    JsonNode json = om.readTree(resp); SoftAssertions softly = new SoftAssertions();
    softly.assertThat(json.path("id").asText(null)).isEqualTo(datasetId.toString())
      .withFailMessage("GET id mismatch. expected=%s actual=%s\nBody:\n%s",
        datasetId, json.path("id").asText(), pretty(resp));
    softly.assertThat(json.path("createdAt").asText(null)).isNotBlank()
      .withFailMessage("GET missing 'createdAt'. Body:\n%s", pretty(resp));
    softly.assertAll();
  }

  @Test @Order(3) @WithMockUser(username="test-user",roles={"USER"})
  void list_shouldBeSortedByCreatedAtDesc() throws Exception {
    assumeCreated();
    MvcResult res = mvc.perform(get("/v1/datasets?limit=5&offset=0"))
      .andExpect(status().isOk()).andReturn();
    String resp = res.getResponse().getContentAsString();
    JsonNode json = om.readTree(resp); SoftAssertions softly = new SoftAssertions();
    softly.assertThat(json.isArray()).withFailMessage("List should be array. Body:\n%s", pretty(resp)).isTrue();
    boolean contains=false; Instant prev=null;
    for(JsonNode item:json){
      if(item.path("id").asText("").equals(String.valueOf(datasetId))) contains=true;
      if(item.hasNonNull("createdAt")){
        Instant cur=Instant.parse(item.get("createdAt").asText());
        if(prev!=null){ softly.assertThat(!cur.isAfter(prev))
          .withFailMessage("Not sorted desc: %s after %s.\nBody:\n%s", cur, prev, pretty(resp)).isTrue();}
        prev=cur;}}
    softly.assertThat(contains).withFailMessage("List doesn't contain created id=%s.\nBody:\n%s", datasetId, pretty(resp)).isTrue();
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
