package com.chessapp.api.ingest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redirects legacy data import endpoint to the new ingest endpoint.
 */
@RestController
public class LegacyDataImportAliasController {

    @PostMapping("/v1/data/import")
    @Operation(summary = "Legacy redirect", responses = {
            @ApiResponse(responseCode = "308", description = "Permanent redirect")
    })
    public ResponseEntity<Void> redirect() {
        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT)
                .header(HttpHeaders.LOCATION, "/v1/ingest")
                .build();
    }
}
