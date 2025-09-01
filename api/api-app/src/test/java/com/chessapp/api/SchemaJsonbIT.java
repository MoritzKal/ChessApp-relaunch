package com.chessapp.api;

import com.chessapp.api.dataset.DatasetEntity;
import com.chessapp.api.domain.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SchemaJsonbIT {

    @Autowired
    EntityManager em;

    @Test
    @Transactional
    void dataset_jsonb_roundtrip() {
        DatasetEntity d = new DatasetEntity();
        d.setName("rt");
        d.setVersion("v1");
        d.setFilterJson("{\"k\":\"v\"}");
        d.setSplitJson("{\"a\":1}");
        em.persist(d);
        em.flush();
        UUID id = d.getId();
        em.clear();

        DatasetEntity d2 = em.find(DatasetEntity.class, id);
        assertThat(d2).isNotNull();
        assertThat(d2.getFilterJson()).contains("\"k\":\"v\"");
        assertThat(d2.getSplitJson()).contains("\"a\":1");
    }

    @Test
    @Transactional
    void game_tags_roundtrip() {
        Game g = new Game();
        g.setId(UUID.randomUUID());
        g.setUserId(UUID.randomUUID());
        g.setPlatform(Platform.CHESS_COM);
        g.setEndTime(Instant.now());
        g.setTimeControl("600");
        g.setPgn("1.e4 e5 2.Nf3 Nc6");
        Map<String, Object> tags = new HashMap<>();
        tags.put("color", "WHITE");
        tags.put("event", "Test");
        g.setTags(tags);

        em.persist(g);
        em.flush();
        UUID id = g.getId();
        em.clear();

        Game g2 = em.find(Game.class, id);
        assertThat(g2).isNotNull();
        assertThat(g2.getTags().get("event")).isEqualTo("Test");
    }

    @Test
    @Transactional
    void position_legal_moves_roundtrip() {
        Position p = new Position();
        p.setId(UUID.randomUUID());
        p.setGameId(UUID.randomUUID());
        p.setPly(1);
        p.setFen("startpos");
        p.setSideToMove("W");
        p.setLegalMoves(new ArrayList<>(List.of("e4", "d4")));
        em.persist(p);
        em.flush();
        UUID id = p.getId();
        em.clear();

        Position p2 = em.find(Position.class, id);
        assertThat(p2).isNotNull();
        assertThat(p2.getLegalMoves()).contains("e4", "d4");
        assertThat(p2.getSideToMove()).isEqualTo("W");
    }

    @Test
    @Transactional
    void model_metrics_roundtrip() {
        Model m = new Model();
        m.setId(UUID.randomUUID());
        m.setName("m");
        m.setVersion("1");
        m.setFramework("tf");
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("acc", 0.9);
        m.setMetrics(metrics);
        em.persist(m);
        em.flush();
        UUID id = m.getId();
        em.clear();

        Model m2 = em.find(Model.class, id);
        assertThat(m2.getMetrics().get("acc")).isEqualTo(0.9);
    }

    @Test
    @Transactional
    void training_run_jsonb_roundtrip() {
        TrainingRun tr = new TrainingRun();
        tr.setId(UUID.randomUUID());
        tr.setStatus(TrainingStatus.QUEUED);
        tr.setParams(new HashMap<>(Map.of("lr", 0.01)));
        tr.setMetrics(new HashMap<>(Map.of("loss", 1.23)));
        em.persist(tr);
        em.flush();
        UUID id = tr.getId();
        em.clear();

        TrainingRun tr2 = em.find(TrainingRun.class, id);
        assertThat(tr2.getParams().get("lr")).isEqualTo(0.01);
        assertThat(tr2.getMetrics().get("loss")).isEqualTo(1.23);
        assertThat(tr2.getStatus()).isNotNull();
    }

    @Test
    @Transactional
    void evaluation_metric_suite_roundtrip() {
        Evaluation e = new Evaluation();
        e.setId(UUID.randomUUID());
        e.setMetricSuite(new HashMap<>(Map.of("f1", 0.8)));
        em.persist(e);
        em.flush();
        UUID id = e.getId();
        em.clear();

        Evaluation e2 = em.find(Evaluation.class, id);
        assertThat(e2.getMetricSuite().get("f1")).isEqualTo(0.8);
    }
}
