package com.chessapp.api.ingest.api;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/v1/data")
public class DataImportAliasController {

    @PostMapping("/import")
    @Operation(hidden = true)
    public ResponseEntity<Void> alias() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .location(URI.create("/v1/ingest"))
                .build();
    }
}
