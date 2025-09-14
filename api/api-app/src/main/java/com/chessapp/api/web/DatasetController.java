package com.chessapp.api.web;

import java.util.Comparator;
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
import com.chessapp.api.domain.entity.Dataset;
import com.chessapp.api.domain.entity.DatasetVersion;
import com.chessapp.api.domain.repo.DatasetRepository;
import com.chessapp.api.domain.repo.DatasetVersionRepository;
import com.chessapp.api.data.ingest.IngestRunRepository;
import com.chessapp.api.data.ingest.IngestRunEntity;
import com.chessapp.api.service.DatasetService;
import com.chessapp.api.datasets.service.DatasetProfileService;
import com.chessapp.api.service.dto.DatasetCreateRequest;
import com.chessapp.api.service.dto.DatasetResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/datasets")
public class DatasetController {

    private final DatasetService datasetService;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository versionRepository;
    private final IngestRunRepository ingestRepository;
    private final DatasetProfileService profileService;

    public DatasetController(DatasetService datasetService,
                             DatasetRepository datasetRepository,
                             DatasetVersionRepository versionRepository,
                             IngestRunRepository ingestRepository,
                             DatasetProfileService profileService) {
        this.datasetService = datasetService;
        this.datasetRepository = datasetRepository;
        this.versionRepository = versionRepository;
        this.ingestRepository = ingestRepository;
        this.profileService = profileService;
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
            return ResponseEntity.ok(new SummaryDto("sample", 0L, 0L, 0, List.of(), null));
        }
        // Resolve dataset by UUID or name
        Dataset dataset = null;
        try {
            dataset = datasetRepository.findById(UUID.fromString(id)).orElse(null);
        } catch (IllegalArgumentException ignored) {
            dataset = datasetRepository.findByNameIgnoreCase(id).orElse(null);
        }
        if (dataset == null) {
            // Fallback to service-based lookup to preserve existing behavior in tests
            try {
                var d = datasetService.get(UUID.fromString(id));
                long rows = d.getSizeRows() != null ? d.getSizeRows() : 0L;
                String v = d.getVersion() != null ? d.getVersion() : null;
                String updatedAt = d.getCreatedAt() != null ? d.getCreatedAt().toString() : null;
                return ResponseEntity.ok(new SummaryDto(d.getId().toString(), rows, 0L, 0,
                        v != null ? List.of(v) : List.of(), updatedAt));
            } catch (Exception e) {
                return ResponseEntity.notFound().build();
            }
        }
        List<DatasetVersion> versions = versionRepository.findAllByDatasetIdOrderByVersionDesc(dataset.getId());
        long rows = versions.stream().map(v -> v.getRows() == null ? 0L : v.getRows()).reduce(0L, Long::sum);
        long sizeBytes = versions.stream().map(v -> v.getSizeBytes() == null ? 0L : v.getSizeBytes()).reduce(0L, Long::sum);
        String updatedAt = versions.stream()
                .map(v -> v.getUpdatedAt() != null ? v.getUpdatedAt() : v.getCreatedAt())
                .filter(java.util.Objects::nonNull)
                .max(java.time.Instant::compareTo)
                .map(java.time.Instant::toString)
                .orElse(dataset.getUpdatedAt() != null ? dataset.getUpdatedAt().toString() : null);
        List<String> versionList = versions.stream().map(DatasetVersion::getVersion).toList();
        return ResponseEntity.ok(new SummaryDto(
                dataset.getId().toString(), rows, sizeBytes, 0, versionList, updatedAt));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<VersionsDto> versions(@PathVariable String id) {
        if ("sample".equalsIgnoreCase(id)) {
            var item = new VersionItemDto("v1", java.time.Instant.now().toString());
            return ResponseEntity.ok(new VersionsDto(1, item.version(), List.of(item)));
        }
        Dataset dataset = null;
        try {
            dataset = datasetRepository.findById(UUID.fromString(id)).orElse(null);
        } catch (IllegalArgumentException ignored) {
            dataset = datasetRepository.findByNameIgnoreCase(id).orElse(null);
        }
        if (dataset == null) {
            return ResponseEntity.notFound().build();
        }
        List<DatasetVersion> versions = versionRepository.findAllByDatasetIdOrderByVersionDesc(dataset.getId());
        if (versions.isEmpty()) {
            return ResponseEntity.ok(new VersionsDto(0, null, List.of()));
        }
        // Ensure latest = highest YYYY-MM (DESC by version string)
        versions.sort(Comparator.comparing(DatasetVersion::getVersion).reversed());
        String latest = versions.get(0).getVersion();
        List<VersionItemDto> items = versions.stream()
                .map(v -> new VersionItemDto(v.getVersion(),
                        (v.getCreatedAt() != null ? v.getCreatedAt() : v.getUpdatedAt()).toString()))
                .toList();
        return ResponseEntity.ok(new VersionsDto(items.size(), latest, items));
    }

    @GetMapping("/{id}/schema")
    public ResponseEntity<SchemaDto> schema(@PathVariable String id,
                                            @RequestParam(name="version", required=false) String version) {
        // Provide a minimal schema based on expected chess dataset columns until profiling is available
        // Resolve dataset existence first (by UUID or name)
        Dataset dataset = null;
        try {
            try { dataset = datasetRepository.findById(UUID.fromString(id)).orElse(null); }
            catch (IllegalArgumentException ignored) { dataset = datasetRepository.findByNameIgnoreCase(id).orElse(null); }
            if (dataset == null && !"sample".equalsIgnoreCase(id)) return ResponseEntity.notFound().build();
        } catch (Exception e) { return ResponseEntity.notFound().build(); }
        if (version != null && !version.isBlank()) {
            try {
                SchemaDto fromProfile = profileService.loadSchema(dataset != null ? dataset.getId().toString() : id, version);
                if (fromProfile != null && fromProfile.columns() != null && !fromProfile.columns().isEmpty()) {
                    return ResponseEntity.ok(fromProfile);
                }
            } catch (Exception ignored) { }
        }
        var cols = List.of(
                new ColumnDto("endTime", "timestamp", 0.0, 1.0, null, null),
                new ColumnDto("timeControl", "string", 0.0, 0.4, null, null),
                new ColumnDto("result", "string", 0.0, 0.1, null, null),
                new ColumnDto("whiteRating", "int", 0.0, 0.9, null, null),
                new ColumnDto("blackRating", "int", 0.0, 0.9, null, null),
                new ColumnDto("pgn", "string", 0.0, 1.0, null, null)
        );
        return ResponseEntity.ok(new SchemaDto(cols));
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
    public ResponseEntity<QualityDto> quality(@PathVariable String id,
                                              @RequestParam(name="version", required=false) String version) {
        if ("sample".equalsIgnoreCase(id)) return ResponseEntity.ok(new QualityDto(0.0, 0.0, 0.0));
        Dataset dataset = null;
        try {
            try { dataset = datasetRepository.findById(UUID.fromString(id)).orElse(null); }
            catch (IllegalArgumentException ignored) { dataset = datasetRepository.findByNameIgnoreCase(id).orElse(null); }
            if (dataset == null) return ResponseEntity.notFound().build();
        } catch (Exception e) { return ResponseEntity.notFound().build(); }
        if (version != null && !version.isBlank()) {
            try { return ResponseEntity.ok(profileService.loadQuality(dataset.getId().toString(), version)); }
            catch (Exception ignored) { }
        }
        return ResponseEntity.ok(new QualityDto(0.0, 0.0, 0.0));
    }

    @GetMapping("/{id}/ingest/history")
    public ResponseEntity<IngestHistoryDto> history(@PathVariable String id) {
        // Try to resolve dataset by UUID or name
        String dsId = null;
        if ("sample".equalsIgnoreCase(id)) {
            dsId = "sample";
        } else {
            try {
                Dataset dataset = null;
                try { dataset = datasetRepository.findById(UUID.fromString(id)).orElse(null); }
                catch (IllegalArgumentException ignored) { dataset = datasetRepository.findByNameIgnoreCase(id).orElse(null); }
                if (dataset == null) return ResponseEntity.notFound().build();
                dsId = dataset.getId().toString();
            } catch (Exception e) { return ResponseEntity.notFound().build(); }
        }

        List<IngestHistoryItemDto> items = new java.util.ArrayList<>();
        try {
            // match by dataset UUID string or by dataset name (some runs store name instead of UUID)
            List<IngestRunEntity> runs = new java.util.ArrayList<>();
            runs.addAll(ingestRepository.findAllByDatasetIdOrderByStartedAtDesc(dsId));
            try {
                Dataset d = datasetRepository.findById(java.util.UUID.fromString(dsId)).orElse(null);
                if (d != null) {
                    runs.addAll(ingestRepository.findAllByDatasetIdOrderByStartedAtDesc(d.getName()));
                }
            } catch (Exception ignored) { /* dsId may be name already */ }
            for (IngestRunEntity r : runs) {
                String note = (r.getFromMonth() != null && r.getToMonth() != null)
                        ? (r.getFromMonth() + ".." + r.getToMonth()) : null;
                items.add(new IngestHistoryItemDto(
                        (r.getFinishedAt() != null ? r.getFinishedAt() : r.getStartedAt()).toString(),
                        r.getUsername(),
                        note,
                        r.getVersion()
                ));
            }
        } catch (Exception ignored) { /* default to empty */ }
        return ResponseEntity.ok(new IngestHistoryDto(items));
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
