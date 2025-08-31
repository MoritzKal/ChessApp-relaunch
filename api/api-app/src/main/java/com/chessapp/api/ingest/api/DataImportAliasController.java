package com.chessapp.api.ingest.api;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataImportAliasController {

    @PostMapping("/v1/data/import")
    @Operation(deprecated = true, description = "Alias – 308 → /v1/ingest")
    public ResponseEntity<Void> importAlias() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .header(HttpHeaders.LOCATION, "/v1/ingest")
                .build();
    }
}
