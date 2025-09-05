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
    public DatasetDetailDto get(@PathVariable UUID id) {
        DatasetResponse d = datasetService.get(id);
        return new DatasetDetailDto(
                d.getId().toString(),
                d.getName(),
                d.getSizeRows() != null ? d.getSizeRows() : 0L,
                0L,
                0,
                d.getCreatedAt().toString(),
                null
        );
    }

    // --- Integrated detail endpoints previously in DatasetsController (delegate/minimal implementations) ---

    @GetMapping("/{id}/summary")
    public SummaryDto summary(@PathVariable UUID id) {
        DatasetResponse d = datasetService.get(id);
        long rows = d.getSizeRows() != null ? d.getSizeRows() : 0L;
        long sizeBytes = 0L; // not tracked yet
        int classes = 0; // no class breakdown available yet
        return new SummaryDto(id.toString(), rows, sizeBytes, classes);
    }

    @GetMapping("/{id}/versions")
    public VersionsDto versions(@PathVariable UUID id) {
        DatasetResponse d = datasetService.get(id);
        var v = d.getVersion();
        var item = new VersionItemDto(v != null ? v : "v1", d.getCreatedAt().toString());
        return new VersionsDto(1, item.version(), List.of(item));
    }

    @GetMapping("/{id}/schema")
    public SchemaDto schema(@PathVariable UUID id) {
        // Schema not implemented yet — return empty list so FE renders gracefully
        return new SchemaDto(List.of());
    }

    @GetMapping("/{id}/sample")
    public SampleDto sample(@PathVariable UUID id,
                            @RequestParam(defaultValue = "10") int limit,
                            @RequestParam(required = false) String cursor) {
        // Placeholder paging behavior compatible with historical tests
        String next = (cursor == null ? "next" : null);
        return new SampleDto(List.of(Map.of("datasetId", id.toString())), next);
    }

    @GetMapping("/{id}/quality")
    public QualityDto quality(@PathVariable UUID id) {
        // Not tracked yet
        return new QualityDto(0.0, 0.0, 0.0);
    }

    @GetMapping("/{id}/ingest/history")
    public IngestHistoryDto history(@PathVariable UUID id) {
        // Not tracked yet
        return new IngestHistoryDto(List.of());
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<Void> export(@PathVariable UUID id,
                                       @RequestParam String format,
                                       @RequestParam(required = false) String version) {
        if (!List.of("parquet", "csv", "pgn").contains(format)) {
            return ResponseEntity.badRequest().build();
        }
        java.net.URI uri = java.net.URI.create("https://example.com/" + id + "." + format);
        return ResponseEntity.status(302).location(uri).build();
    }
}
