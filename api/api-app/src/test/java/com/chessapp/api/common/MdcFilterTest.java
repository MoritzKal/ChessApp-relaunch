package com.chessapp.api.common;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MdcFilterTest {
    @Test
    void populatesAndClearsMdc() throws ServletException, IOException {
        MdcFilter f = new MdcFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("X-Run-Id", "run123");
        req.addHeader("X-Dataset-Id", "ds456");
        req.addHeader("X-Model-Id", "m789");
        req.addHeader("X-User", "alice");

        jakarta.servlet.FilterChain chain = (request, response) -> {
            assertThat(MDC.get("run_id")).isEqualTo("run123");
            assertThat(MDC.get("dataset_id")).isEqualTo("ds456");
            assertThat(MDC.get("model_id")).isEqualTo("m789");
            assertThat(MDC.get("username")).isEqualTo("alice");
            assertThat(MDC.get("component")).isEqualTo("api");
        };

        f.doFilter(req, res, chain);
        assertThat(MDC.get("run_id")).isNull();
        assertThat(MDC.get("component")).isNull();
    }
}
