package com.chessapp.api.models.service;

import com.chessapp.api.domain.entity.Model;
import com.chessapp.api.domain.repo.ModelRepository;
import io.micrometer.core.instrument.MeterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ModelService {
    private static final Logger log = LoggerFactory.getLogger(ModelService.class);
    private final ModelRepository repo;
    private final MeterRegistry metrics;
    private final ObjectMapper mapper;

    public ModelService(ModelRepository repo, MeterRegistry metrics, ObjectMapper mapper) {
        this.repo = repo;
        this.metrics = metrics;
        this.mapper = mapper;
    }

    public List<Model> list() {
        return repo.findAll();
    }

    public Model get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ModelNotFoundException(id.toString()));
    }

    @Transactional
    public void promote(UUID id, String actor) {
        MDC.put("model_id", id.toString());
        Model target = get(id);
        UUID prevProd = repo.findByIsProdTrue().map(Model::getId).orElse(null);
        if (target.isProd()) {
            metrics.counter("chs_model_promotions_total", "result", "noop").increment();
            audit("noop", id, actor, prevProd);
            MDC.remove("model_id");
            return;
        }
        repo.clearProd();
        repo.markProd(id);
        metrics.counter("chs_model_promotions_total", "result", "success").increment();
        audit("success", id, actor, prevProd);
        MDC.remove("model_id");
    }

    private void audit(String result, UUID modelId, String actor, UUID prevProd) {
        try {
            var evt = Map.of(
                    "event", "MODEL_PROMOTED",
                    "result", result,
                    "model_id", modelId.toString(),
                    "actor", actor,
                    "prev_prod_id", prevProd == null ? null : prevProd.toString(),
                    "ts", Instant.now().toString());
            log.info(mapper.writeValueAsString(evt));
        } catch (JsonProcessingException e) {
            log.warn("audit_log_failed", e);
        }
    }
}
