package com.chessapp.api.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.chessapp.api.common.dto.CountDto;
import com.chessapp.api.datasets.api.dto.*;
import com.chessapp.api.service.DatasetService;
import com.chessapp.api.service.dto.DatasetCreateRequest;
import com.chessapp.api.service.dto.DatasetResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetResponse create(@Valid @RequestBody DatasetCreateRequest request) {
        return datasetService.create(request);
    }

    @GetMapping
    public DatasetListDto list(@RequestParam(defaultValue = "20") int limit,
                               @RequestParam(defaultValue = "0") int offset,
                               @RequestParam(required = false) String sort,
                               @RequestParam(required = false) String q) {
        if (limit < 1) limit = 1;
        if (limit > 100) limit = 100;
        List<DatasetListItemDto> items = datasetService.list(limit, offset, sort, q).stream()
                .map(d -> new DatasetListItemDto(
                        d.getId().toString(),
                        d.getName(),
                        d.getSizeRows() != null ? d.getSizeRows() : 0L,
                        0L,
                        new VersionsSummaryDto(1, d.getVersion()),
                        d.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
        Integer nextOffset = items.size() == limit ? offset + limit : null;
        return new DatasetListDto(items, nextOffset);
    }

    @GetMapping("/count")
    public CountDto count() {
        return new CountDto(datasetService.count());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatasetDetailDto> get(@PathVariable String id) {
        DatasetResponse d;
        try {
            UUID uuid = UUID.fromString(id);
            d = datasetService.get(uuid);
        } catch (IllegalArgumentException e) {
            try {
                d = datasetService.getByName(id);
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(new DatasetDetailDto(
                d.getId().toString(),
                d.getName(),
                d.getSizeRows() != null ? d.getSizeRows() : 0L,
                0L,
                0,
                d.getCreatedAt().toString(),
                null
        ));
    }

    @GetMapping("/byName/{name}")
    public ResponseEntity<DatasetDetailDto> getByName(@PathVariable String name) {
        try {
            DatasetResponse d = datasetService.getByName(name);
            return ResponseEntity.ok(new DatasetDetailDto(
                    d.getId().toString(),
                    d.getName(),
                    d.getSizeRows() != null ? d.getSizeRows() : 0L,
                    0L,
                    0,
                    d.getCreatedAt().toString(),
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // --- Integrated detail endpoints previously in DatasetsController (delegate/minimal implementations) ---

    @GetMapping("/{id}/summary")
    public ResponseEntity<SummaryDto> summary(@PathVariable String id) {
        // Allow special demo id "sample"
        if ("sample".equalsIgnoreCase(id)) {
            return ResponseEntity.ok(new SummaryDto("sample", 0L, 0L, 0));
        }
        try {
            UUID uuid = UUID.fromString(id);
            DatasetResponse d = datasetService.get(uuid);
            long rows = d.getSizeRows() != null ? d.getSizeRows() : 0L;
            long sizeBytes = 0L; // not tracked yet
            int classes = 0; // no class breakdown available yet
            return ResponseEntity.ok(new SummaryDto(uuid.toString(), rows, sizeBytes, classes));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<VersionsDto> versions(@PathVariable String id) {
        if ("sample".equalsIgnoreCase(id)) {
            var item = new VersionItemDto("v1", java.time.Instant.now().toString());
            return ResponseEntity.ok(new VersionsDto(1, item.version(), List.of(item)));
        }
        try {
            UUID uuid = UUID.fromString(id);
            DatasetResponse d = datasetService.get(uuid);
            var v = d.getVersion();
            var item = new VersionItemDto(v != null ? v : "v1", d.getCreatedAt().toString());
            return ResponseEntity.ok(new VersionsDto(1, item.version(), List.of(item)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/schema")
    public ResponseEntity<SchemaDto> schema(@PathVariable String id) {
        // Not implemented yet; return empty schema for known ids
        if ("sample".equalsIgnoreCase(id)) return ResponseEntity.ok(new SchemaDto(List.of()));
        try { UUID.fromString(id); return ResponseEntity.ok(new SchemaDto(List.of())); }
        catch (Exception e) { return ResponseEntity.notFound().build(); }
    }

    @GetMapping("/{id}/sample")
    public ResponseEntity<SampleDto> sample(@PathVariable String id,
                            @RequestParam(defaultValue = "10") int limit,
                            @RequestParam(required = false) String cursor) {
        String next = (cursor == null ? "next" : null);
        if ("sample".equalsIgnoreCase(id)) {
            return ResponseEntity.ok(new SampleDto(List.of(Map.of("datasetId", id)), next));
        }
        try { UUID.fromString(id); return ResponseEntity.ok(new SampleDto(List.of(Map.of("datasetId", id)), next)); }
        catch (Exception e) { return ResponseEntity.notFound().build(); }
    }

    @GetMapping("/{id}/quality")
    public ResponseEntity<QualityDto> quality(@PathVariable String id) {
        if ("sample".equalsIgnoreCase(id)) return ResponseEntity.ok(new QualityDto(0.0, 0.0, 0.0));
        try { UUID.fromString(id); return ResponseEntity.ok(new QualityDto(0.0, 0.0, 0.0)); }
        catch (Exception e) { return ResponseEntity.notFound().build(); }
    }

    @GetMapping("/{id}/ingest/history")
    public ResponseEntity<IngestHistoryDto> history(@PathVariable String id) {
        if ("sample".equalsIgnoreCase(id)) return ResponseEntity.ok(new IngestHistoryDto(List.of()));
        try { UUID.fromString(id); return ResponseEntity.ok(new IngestHistoryDto(List.of())); }
        catch (Exception e) { return ResponseEntity.notFound().build(); }
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<Void> export(@PathVariable String id,
                                       @RequestParam String format,
                                       @RequestParam(required = false) String version) {
        if (!List.of("parquet", "csv", "pgn").contains(format)) {
            return ResponseEntity.badRequest().build();
        }
        // Allow exporting sample or UUIDs; other ids => 404
        if (!"sample".equalsIgnoreCase(id)) {
            try { UUID.fromString(id); } catch (Exception e) { return ResponseEntity.notFound().build(); }
        }
        java.net.URI uri = java.net.URI.create("https://example.com/" + id + "." + format);
        return ResponseEntity.status(302).location(uri).build();
    }
}
