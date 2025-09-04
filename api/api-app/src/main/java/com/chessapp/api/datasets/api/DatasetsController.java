package com.chessapp.api.datasets.api;

import com.chessapp.api.datasets.api.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/datasets")
public class DatasetsController {

    @GetMapping("/{id}/summary")
    public SummaryDto summary(@PathVariable String id) {
        return new SummaryDto(id, 0L, 0L, 0);
    }

    @GetMapping("/{id}/versions")
    public VersionsDto versions(@PathVariable String id) {
        var item = new VersionItemDto("v1", Instant.now().toString());
        return new VersionsDto(1, "v1", List.of(item));
    }

    @GetMapping("/{id}/schema")
    public SchemaDto schema(@PathVariable String id) {
        var col = new ColumnDto("col", "string", 0.0, 0.0, null, null);
        return new SchemaDto(List.of(col));
    }

    @GetMapping("/{id}/sample")
    public SampleDto sample(@PathVariable String id,
                             @RequestParam(defaultValue="10") int limit,
                             @RequestParam(required=false) String cursor) {
        String next = cursor == null ? "next" : null;
        return new SampleDto(List.of(Map.of("id", id)), next);
    }

    @GetMapping("/{id}/quality")
    public QualityDto quality(@PathVariable String id) {
        return new QualityDto(0.0, 0.0, 0.0);
    }

    @GetMapping("/{id}/ingest/history")
    public IngestHistoryDto history(@PathVariable String id) {
        return new IngestHistoryDto(List.of());
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<Void> export(@PathVariable String id,
                                       @RequestParam String format,
                                       @RequestParam(required = false) String version) {
        if (!List.of("parquet", "csv", "pgn").contains(format)) {
            return ResponseEntity.badRequest().build();
        }
        URI uri = URI.create("https://example.com/" + id + "." + format);
        return ResponseEntity.status(302).location(uri).build();
    }
}
