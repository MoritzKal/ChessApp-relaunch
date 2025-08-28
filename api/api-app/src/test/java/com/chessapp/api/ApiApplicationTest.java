package com.chessapp.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"})
@org.springframework.test.context.ActiveProfiles("codex")
class ApiApplicationTest extends com.chessapp.api.testutil.AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
