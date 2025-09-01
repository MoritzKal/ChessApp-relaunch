package com.chessapp.api.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.chessapp.api.service.DatasetService;
import com.chessapp.api.service.dto.DatasetCreateRequest;
import com.chessapp.api.service.dto.DatasetResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/datasets")
@Tag(name = "Datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping
    @Operation(summary = "Create dataset")
    public ResponseEntity<DatasetResponse> create(@Valid @RequestBody DatasetCreateRequest request) {
        DatasetResponse resp = datasetService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resp.getId())
                .toUri();
        return ResponseEntity.created(location).body(resp);
    }

    @GetMapping
    @Operation(summary = "List datasets")
    public List<DatasetResponse> list(@RequestParam(defaultValue = "50") int limit,
                                      @RequestParam(defaultValue = "0") int offset) {
        return datasetService.list(limit, offset);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dataset by id")
    public DatasetResponse get(@PathVariable UUID id) {
        return datasetService.get(id);
    }
}
