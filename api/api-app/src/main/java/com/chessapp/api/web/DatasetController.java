package com.chessapp.api.web;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
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
}
