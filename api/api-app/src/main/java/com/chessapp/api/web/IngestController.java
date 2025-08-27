package com.chessapp.api.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.chessapp.api.ingest.service.IngestService;

@RestController
@RequestMapping("/v1/data/import")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void startImport() {
        ingestService.startImport();
    }
}
