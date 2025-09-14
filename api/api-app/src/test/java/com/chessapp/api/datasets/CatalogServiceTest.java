package com.chessapp.api.datasets;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;
import com.chessapp.api.codex.CodexApplication;
import com.chessapp.api.datasets.service.DatasetCatalogService;
import com.chessapp.api.domain.repo.DatasetRepository;
import com.chessapp.api.domain.repo.DatasetVersionRepository;
import com.chessapp.api.testutil.AbstractIntegrationTest;
import com.chessapp.api.testutil.TestAuth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"},
        classes = CodexApplication.class)
@AutoConfigureMockMvc
class CatalogServiceTest extends AbstractIntegrationTest {

    @Autowired DatasetCatalogService catalog;
    @Autowired DatasetRepository datasetRepo;
    @Autowired DatasetVersionRepository versionRepo;
    @Autowired MockMvc mvc;
    @MockitoBean S3Client s3;

    @Test
    void register_parallel() throws Exception {
        String name = "ds_par";
        CountDownLatch latch = new CountDownLatch(2);
        Runnable r = () -> { catalog.registerIfAbsent(name, name); latch.countDown(); };
        var t1 = new Thread(r); var t2 = new Thread(r);
        t1.start(); t2.start();
        latch.await();
        long count = datasetRepo.findAll().stream().filter(d -> name.equals(d.getName())).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void addVersion_idempotent() {
        String name = "ds_idem";
        catalog.registerIfAbsent(name, name);
        catalog.addVersion(name, "v1", 1, 1);
        var v1 = versionRepo.findAll().get(0);
        Instant first = v1.getUpdatedAt();
        catalog.addVersion(name, "v1", 2, 2);
        var v2 = versionRepo.findByDatasetIdAndVersion(v1.getDatasetId(), "v1").orElseThrow();
        assertThat(v2.getUpdatedAt()).isAfterOrEqualTo(first);
    }

    @Test
    void controller_get_registered() throws Exception {
        String name = "chesscom_demo";
        catalog.registerIfAbsent(name, name);
        mvc.perform(get("/v1/datasets/" + name).with(TestAuth.jwtUser()))
                .andExpect(status().isOk());
    }
}
