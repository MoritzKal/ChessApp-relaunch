package com.chessapp.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.chessapp.api.domain.entity.Dataset;
import com.chessapp.api.domain.repo.DatasetRepository;

@SpringBootTest(properties = {"logging.config=classpath:logback-spring.xml"})
class RepositoryCrudTest extends com.chessapp.api.testutil.AbstractIntegrationTest {

    @Autowired
    DatasetRepository datasetRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void datasetInsertAndRead() {
        Dataset ds = new Dataset();
        ds.setId(UUID.randomUUID());
        ds.setName("test_ds");
        ds.setVersion("v1");
        ds.setFilter(Map.of());
        ds.setSplit(Map.of());
        ds.setSizeRows(1L);
        ds.setLocationUri("s3://datasets/test_ds_v1");
        ds.setCreatedAt(Instant.now());

        datasetRepository.save(ds);

        Dataset found = datasetRepository.findById(ds.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("test_ds");
    }

    @Test
    void seededGamePresent() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM games WHERE game_id_ext = 'demo-0001'",
            Integer.class);
        assertThat(count).isNotNull();
        assertThat(count).isGreaterThan(0);
    }
}
