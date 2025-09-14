package com.chessapp.api.datasets.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.chessapp.api.storage.MinioStorageService;
import com.chessapp.api.datasets.api.dto.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class DatasetProfileService {
    private final MinioStorageService storage;
    private final ObjectMapper mapper;

    public DatasetProfileService(MinioStorageService storage, ObjectMapper mapper) {
        this.storage = storage;
        this.mapper = mapper;
    }

    public SchemaDto loadSchema(String datasetId, String version) {
        // Heuristics: try common paths under mlflow bucket
        String[] candidates = new String[] {
                "s3://mlflow/reports/dataset/" + datasetId + "/" + version + "/profile.json",
                "s3://mlflow/datasets/" + datasetId + "/" + version + "/profile.json",
        };
        for (String uri : candidates) {
            try {
                byte[] data = storage.readUri(uri);
                if (data != null && data.length > 0) {
                    JsonNode root = mapper.readTree(data);
                    List<ColumnDto> cols = new ArrayList<>();
                    // Expect a 'schema.columns' array with name/type/min/max/nullPct/uniquePct
                    JsonNode arr = root.at("/schema/columns");
                    if (arr != null && arr.isArray()) {
                        for (JsonNode c : arr) {
                            String name = text(c, "name");
                            String dtype = text(c, "type");
                            double nullPct = num(c, "nullPct");
                            double uniquePct = num(c, "uniquePct");
                            String min = text(c, "min");
                            String max = text(c, "max");
                            cols.add(new ColumnDto(name, dtype, nullPct, uniquePct, min, max));
                        }
                    }
                    if (!cols.isEmpty()) return new SchemaDto(cols);
                }
            } catch (Exception ignored) { }
        }
        return new SchemaDto(List.of());
    }

    public QualityDto loadQuality(String datasetId, String version) {
        String[] candidates = new String[] {
                "s3://mlflow/reports/dataset/" + datasetId + "/" + version + "/profile.json",
                "s3://mlflow/datasets/" + datasetId + "/" + version + "/profile.json",
        };
        for (String uri : candidates) {
            try {
                byte[] data = storage.readUri(uri);
                if (data != null && data.length > 0) {
                    JsonNode root = mapper.readTree(data);
                    JsonNode q = root.at("/quality");
                    if (q != null && q.isObject()) {
                        double missing = num(q, "missingPct");
                        double outlier = num(q, "outlierPct");
                        double dup = num(q, "duplicatePct");
                        return new QualityDto(missing, outlier, dup);
                    }
                }
            } catch (Exception ignored) { }
        }
        return new QualityDto(0.0, 0.0, 0.0);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }
    private static double num(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v != null && v.isNumber()) ? v.asDouble() : 0.0;
    }
}

