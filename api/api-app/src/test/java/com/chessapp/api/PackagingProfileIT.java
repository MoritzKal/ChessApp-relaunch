package com.chessapp.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

class PackagingProfileIT {

  @Test
  @DisplayName("Default (prod-like): ApiApplication Context lädt")
  @SpringBootTest(
      classes = com.chessapp.api.ApiApplication.class,
      webEnvironment = SpringBootTest.WebEnvironment.MOCK
  )
  void apiApplicationLoads(@Autowired ApplicationContext ctx) {
    assertThat(ctx).isNotNull();
  }

  @Test
  @DisplayName("Codex-Profil: CodexApplication Context lädt")
  @SpringBootTest(
      classes = com.chessapp.api.codex.CodexApplication.class,
      webEnvironment = SpringBootTest.WebEnvironment.MOCK
  )
  @ActiveProfiles("codex")
  void codexApplicationLoads(@Autowired ApplicationContext ctx) {
    assertThat(ctx).isNotNull();
  }
}

