package com.chessapp.api.models.service;

import com.chessapp.api.models.api.dto.ModelSummary;
import com.chessapp.api.models.api.dto.ModelVersionSummary;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ModelRegistryService {

    private final ObjectMapper mapper;
    private final String registryPath;
    private final MeterRegistry meters;
    private volatile Registry cached;

    public ModelRegistryService(ObjectMapper mapper, @Value("${chs.registry.path:}") String path, MeterRegistry meters) {
        this.mapper = mapper;
        this.registryPath = path;
        this.meters = meters;
        tryLoad();
    }

    public List<ModelSummary> listModels() {
        Registry reg = get();
        List<ModelSummary> out = new ArrayList<>();
        for (Model m : reg.models) {
            out.add(new ModelSummary(m.modelId, m.displayName, m.tags == null ? List.of() : m.tags));
        }
        return out;
    }

    public List<ModelVersionSummary> listVersions(String modelId) {
        Registry reg = get();
        for (Model m : reg.models) {
            if (m.modelId.equals(modelId)) {
                List<ModelVersionSummary> out = new ArrayList<>();
                if (m.versions != null) {
                    for (ModelVersion v : m.versions) {
                        out.add(new ModelVersionSummary(v.modelVersion, v.createdAt, v.metrics));
                    }
                }
                return out;
            }
        }
        throw new ModelNotFoundException(modelId);
    }

    private Registry get() {
        return cached != null ? cached : tryLoad();
    }

    private synchronized Registry tryLoad() {
        try {
            Registry reg = load();
            cached = reg;
            registerGauges(reg);
            return reg;
        } catch (IOException e) {
            throw new RegistryUnavailableException("Failed to load registry", e);
        }
    }

    private Registry load() throws IOException {
        if (registryPath != null && !registryPath.isBlank()) {
            String p = registryPath.trim();
            if (p.startsWith("classpath:")) {
                String cp = p.substring("classpath:".length());
                if (cp.startsWith("/")) cp = cp.substring(1);
                ClassPathResource res = new ClassPathResource(cp);
                if (!res.exists()) {
                    throw new IOException("Classpath registry not found at " + cp);
                }
                try (InputStream in = res.getInputStream()) {
                    return mapper.readValue(in, Registry.class);
                }
            } else {
                File f = new File(p);
                if (!f.exists() || !f.isFile()) {
                    throw new IOException("Registry file not found: " + registryPath);
                }
                try (FileInputStream in = new FileInputStream(f)) {
                    return mapper.readValue(in, Registry.class);
                }
            }
        } else {
            ClassPathResource res = new ClassPathResource("registry/registry.json");
            if (!res.exists()) {
                throw new IOException("Classpath registry not found at registry/registry.json");
            }
            try (InputStream in = res.getInputStream()) {
                return mapper.readValue(in, Registry.class);
            }
        }
    }

    private void registerGauges(Registry reg) {
        for (Model m : reg.models) {
            int c = m.versions == null ? 0 : m.versions.size();
            Gauge.builder("chs_model_registry_versions_total", () -> c)
                    .tag("model_id", m.modelId)
                    .register(meters);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Registry {
        @JsonProperty("models")
        public List<Model> models = Collections.emptyList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Model {
        public String modelId;
        public String displayName;
        public List<String> tags;
        public List<ModelVersion> versions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ModelVersion {
        public String modelVersion;
        public Instant createdAt;
        public Map<String, Object> metrics;
    }
}
