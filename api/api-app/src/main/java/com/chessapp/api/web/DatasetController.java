package com.chessapp.api.web;

import java.util.List;
import java.util.UUID;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<DatasetResponse> create(@Valid @RequestBody DatasetCreateRequest request) {
        DatasetResponse resp = datasetService.create(request);
        return ResponseEntity.created(URI.create("/v1/datasets/" + resp.getId())).body(resp);
    }

    @GetMapping
    public List<DatasetResponse> list(@RequestParam(defaultValue = "50") int limit,
                                      @RequestParam(defaultValue = "0") int offset) {
        return datasetService.list(limit, offset);
    }

    @GetMapping("/{id}")
    public DatasetResponse get(@PathVariable UUID id) {
        return datasetService.get(id);
    }
}
