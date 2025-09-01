package com.chessapp.api.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.chessapp.api.service.DatasetService;
import com.chessapp.api.service.dto.CreateDatasetRequest;
import com.chessapp.api.service.dto.DatasetResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/datasets")
@Tag(name = "datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping
    @Operation(summary = "Create dataset")
    public ResponseEntity<DatasetResponse> create(@Valid @RequestBody CreateDatasetRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        DatasetResponse res = datasetService.create(request);
        return ResponseEntity.created(
                uriBuilder.path("/v1/datasets/{id}").buildAndExpand(res.getId()).toUri())
                .body(res);
    }

    @GetMapping
    @Operation(summary = "List datasets")
    public Page<DatasetResponse> list(
            @PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return datasetService.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dataset by id")
    public DatasetResponse get(@PathVariable UUID id) {
        return datasetService.get(id);
    }
}
