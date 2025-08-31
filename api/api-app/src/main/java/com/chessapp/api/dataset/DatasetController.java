package com.chessapp.api.dataset;

import java.net.URI;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.chessapp.api.dataset.dto.CreateDatasetRequest;
import com.chessapp.api.dataset.dto.DatasetResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/datasets")
@Tag(name = "Datasets")
public class DatasetController {

    private static final Logger log = LoggerFactory.getLogger(DatasetController.class);

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create dataset")
    public ResponseEntity<DatasetResponse> create(@Valid @RequestBody CreateDatasetRequest request) {
        UUID id = datasetService.create(request);
        MDC.put("dataset_id", id.toString());
        log.info("dataset create name={} version={} sizeRows={} locationUri={}",
                request.getName(), request.getVersion(), request.getSizeRows(), request.getLocationUri());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        DatasetResponse body = datasetService.get(id);
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    @Operation(summary = "List datasets")
    public Page<DatasetResponse> list(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        int pageSize = Math.min(size, 100);
        return datasetService.list(PageRequest.of(page, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dataset by id")
    public DatasetResponse get(@PathVariable UUID id) {
        MDC.put("dataset_id", id.toString());
        return datasetService.get(id);
    }
}
